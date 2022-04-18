package jp.ac.tachibana.food_survey.persistence.session

import scala.collection.SortedMap

import cats.data.{NonEmptyList, NonEmptyMap, NonEmptyVector}
import cats.effect.Async
import cats.syntax.applicative.*
import cats.syntax.traverse.*
import doobie.*
import doobie.implicits.*
import doobie.util.fragments

import jp.ac.tachibana.food_survey.domain.question.Question
import jp.ac.tachibana.food_survey.domain.session.{SessionElement, SessionTemplate}
import jp.ac.tachibana.food_survey.persistence.formats.ParameterInstances.*
import jp.ac.tachibana.food_survey.persistence.formats.SessionInstances.SessionElementPostgresFormat

class PostgresSessionTemplateRepository[F[_]: Async](implicit tr: Transactor[F]) extends SessionTemplateRepository[F]:

  private val getSessionElementsQuery =
    sql"""SELECT element_number, type, question_id, show_duration_seconds FROM "session_template_element"
         |ORDER BY element_number""".stripMargin
      .query[SessionElementPostgresFormat]
      .nel
      .map(_.toNev)

  // todo: errors
  override def getActiveTemplate: F[SessionTemplate] =
    val query = for {
      elements <- getSessionElementsQuery
      // todo: elements validation
      // todo: questions validation
      questionIds = extractRequiredQuestionIds(elements)
      questionsOpt <- NonEmptyVector.fromVector(questionIds).traverse(getQuestionsByIdsQuery)
      questionsMap = questionsOpt.fold(Map.empty[Question.Id, Question])(_.groupBy(_.id).view.mapValues(_.head).toMap)
      sessionElements = fillInQuestions(elements, questionsMap)
    } yield SessionTemplate(sessionElements)
    query.transact(tr)

  private def getQuestionsByIdsQuery(questionIds: NonEmptyVector[Question.Id]) =
    (sql"""SELECT id, type, previous_question_id, text, scale_min_bound_caption, scale_max_bound_caption FROM "question"
         |WHERE """.stripMargin ++ fragments.in(fr"id", questionIds))
      .query[Question]
      .nel

  private def extractRequiredQuestionIds(elements: NonEmptyVector[SessionElementPostgresFormat]): Vector[Question.Id] =
    elements.collect { case SessionElementPostgresFormat.Question(_, questionId, _) =>
      questionId
    }

  private def fillInQuestions(
    elements: NonEmptyVector[SessionElementPostgresFormat],
    questions: Map[Question.Id, Question]): NonEmptyVector[SessionElement] =
    // todo: question error?
    elements.map {
      case SessionElementPostgresFormat.Question(number, questionId, showDuration) =>
        questions(questionId) match {
          case q: Question.Basic =>
            SessionElement.Question.Basic(number, q, showDuration)

          case q: Question.Repeated =>
            SessionElement.Question.Repeated(number, q, showDuration)
        }
      case SessionElementPostgresFormat.QuestionReview(number, questionId, showDuration) =>
        questions(questionId) match {
          case q: Question.Basic =>
            SessionElement.QuestionReview.Basic(number, q, showDuration)

          case q: Question.Repeated =>
            SessionElement.QuestionReview.Repeated(number, q, showDuration)
        }
    }
