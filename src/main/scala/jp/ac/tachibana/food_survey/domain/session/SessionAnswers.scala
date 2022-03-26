package jp.ac.tachibana.food_survey.domain.session

import cats.data.NonEmptyMap
import cats.syntax.eq.*

import jp.ac.tachibana.food_survey.domain.question.{Question, QuestionAnswer}
import jp.ac.tachibana.food_survey.domain.user.User

class SessionAnswers private (
  respondentsCount: Int,
  answers: Map[Question.Id, NonEmptyMap[User.Id, QuestionAnswer]]):

  def provideAnswer(answer: QuestionAnswer): SessionAnswers =
    new SessionAnswers(
      respondentsCount,
      answers.updated(
        answer.questionId,
        answers.get(answer.questionId).fold(NonEmptyMap.one(answer.respondentId, answer))(_.add(answer.respondentId -> answer))))

  def answersCount(questionId: Question.Id): Int =
    answers.get(questionId).fold(0)(_.length)

  def isQuestionAnswered(questionId: Question.Id): Boolean =
    answers.get(questionId).exists(_.length === respondentsCount)

  def isQuestionAnsweredBy(
    questionId: Question.Id,
    respondentId: User.Id): Boolean =
    answers.get(questionId).exists(_.contains(respondentId))

  def toMap: Map[Question.Id, NonEmptyMap[User.Id, QuestionAnswer]] =
    answers

object SessionAnswers:

  def apply(respondentsCount: Int): SessionAnswers =
    new SessionAnswers(respondentsCount, Map.empty)
