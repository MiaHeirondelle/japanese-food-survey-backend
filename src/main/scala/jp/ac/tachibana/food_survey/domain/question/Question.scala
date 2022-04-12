package jp.ac.tachibana.food_survey.domain.question

import cats.instances.string.catsKernelStdOrderForString
import cats.{Order, Show}
import jp.ac.tachibana.food_survey.domain.question.Question.ScaleText
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User

sealed trait Question:
  def id: Question.Id
  def text: String
  def scaleText: ScaleText

object Question:

  case class ScaleText(
    minBoundCaption: String,
    maxBoundCaption: String)

  opaque type Id = String

  object Id:

    implicit val order: Order[Question.Id] =
      catsKernelStdOrderForString

    implicit val ordering: Ordering[Question.Id] =
      order.toOrdering

    implicit val show: Show[Question.Id] =
      Show.fromToString

    def apply(questionId: String): Question.Id = questionId

    extension (questionId: Question.Id) def value: String = questionId

  case class Basic(
    id: Question.Id,
    text: String,
    scaleText: Question.ScaleText)
      extends Question

  case class Repeated(
    id: Question.Id,
    previousQuestionId: Question.Id,
    text: String,
    scaleText: Question.ScaleText)
      extends Question

  extension (question: Question)
    def toAnswer(
      sessionNumber: Session.Number,
      respondentId: User.Id,
      value: Option[QuestionAnswer.ScaleValue],
      comment: Option[QuestionAnswer.Comment]): QuestionAnswer =
      question match {
        case Question.Basic(id, _, _) =>
          QuestionAnswer.Basic(
            sessionNumber,
            id,
            respondentId,
            value,
            comment
          )
        case Question.Repeated(id, previousQuestionId, _, _) =>
          QuestionAnswer.Repeated(
            sessionNumber,
            id,
            respondentId,
            value,
            comment,
            previousQuestionId
          )
      }
