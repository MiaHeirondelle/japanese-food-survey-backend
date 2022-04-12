package jp.ac.tachibana.food_survey.services.session.model

import jp.ac.tachibana.food_survey.domain.question.{Question, QuestionAnswer}

sealed trait InputSessionMessage

object InputSessionMessage:
  case object BeginSession extends InputSessionMessage
  case object ReadyForNextElement extends InputSessionMessage
  case class ProvideAnswer(
    questionId: Question.Id,
    scaleValue: Option[QuestionAnswer.ScaleValue],
    comment: Option[QuestionAnswer.Comment])
      extends InputSessionMessage
