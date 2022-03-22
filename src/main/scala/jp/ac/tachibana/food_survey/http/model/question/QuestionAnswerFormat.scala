package jp.ac.tachibana.food_survey.http.model.question

import io.circe.{Encoder, JsonObject}

import jp.ac.tachibana.food_survey.domain.question.QuestionAnswer

sealed abstract class QuestionAnswerFormat(val `type`: QuestionAnswerTypeFormat)

object QuestionAnswerFormat:

  def fromDomain(answer: QuestionAnswer): QuestionAnswerFormat =
    answer match {
      case QuestionAnswer.Basic(sessionNumber, questionId, respondentId, value, comment) =>
        QuestionAnswerFormat.Basic(
          session_number = sessionNumber.value,
          question_id = questionId.value,
          respondent_id = respondentId.value,
          value = value.value,
          comment = comment.value
        )
      case QuestionAnswer.Repeated(sessionNumber, questionId, respondentId, value, comment, previousQuestionId) =>
        QuestionAnswerFormat.Repeated(
          session_number = sessionNumber.value,
          question_id = questionId.value,
          respondent_id = respondentId.value,
          value = value.value,
          comment = comment.value,
          previous_question_id = previousQuestionId.value
        )
    }

  implicit val encoder: Encoder[QuestionAnswerFormat] =
    Encoder.AsObject.instance { q =>
      val base = q match {
        case b: QuestionAnswerFormat.Basic =>
          Encoder.AsObject[QuestionAnswerFormat.Basic].encodeObject(b)

        case r: QuestionAnswerFormat.Repeated =>
          Encoder.AsObject[QuestionAnswerFormat.Repeated].encodeObject(r)
      }
      base.add("type", Encoder[QuestionAnswerTypeFormat].apply(q.`type`))
    }

  case class Basic(
    session_number: Int,
    question_id: String,
    respondent_id: String,
    value: Int,
    comment: String)
      extends QuestionAnswerFormat(QuestionAnswerTypeFormat.Basic)
      derives Encoder.AsObject

  case class Repeated(
    session_number: Int,
    question_id: String,
    respondent_id: String,
    value: Int,
    comment: String,
    previous_question_id: String)
      extends QuestionAnswerFormat(QuestionAnswerTypeFormat.Repeated)
      derives Encoder.AsObject
