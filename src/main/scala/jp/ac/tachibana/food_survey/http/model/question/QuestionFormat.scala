package jp.ac.tachibana.food_survey.http.model.question

import io.circe.{Encoder, JsonObject}

import jp.ac.tachibana.food_survey.domain.question.Question

sealed abstract class QuestionFormat(val `type`: QuestionTypeFormat)

object QuestionFormat:

  def fromDomain(question: Question): QuestionFormat =
    question match {
      case Question.Basic(id, text, scaleText) =>
        QuestionFormat.Basic(
          id = id.value,
          text = text,
          scale_text = QuestionFormat.ScaleText.fromDomain(scaleText)
        )
      case Question.Repeated(id, previousQuestionId, text, scaleText) =>
        QuestionFormat.Repeated(
          id = id.value,
          previous_question_id = previousQuestionId.value,
          text = text,
          scale_text = QuestionFormat.ScaleText.fromDomain(scaleText)
        )
    }

  implicit val encoder: Encoder[QuestionFormat] =
    Encoder.AsObject.instance { q =>
      val base = q match {
        case b: QuestionFormat.Basic =>
          Encoder.AsObject[QuestionFormat.Basic].encodeObject(b)

        case r: QuestionFormat.Repeated =>
          Encoder.AsObject[QuestionFormat.Repeated].encodeObject(r)
      }
      base.add("type", Encoder[QuestionTypeFormat].apply(q.`type`))
    }

  case class Basic(
    id: String,
    text: String,
    scale_text: QuestionFormat.ScaleText)
      extends QuestionFormat(QuestionTypeFormat.Basic)
      derives Encoder.AsObject

  case class Repeated(
    id: String,
    previous_question_id: String,
    text: String,
    scale_text: QuestionFormat.ScaleText)
      extends QuestionFormat(QuestionTypeFormat.Repeated)
      derives Encoder.AsObject

  case class ScaleText(
    min_bound_caption: String,
    max_bound_caption: String)
      derives Encoder.AsObject

  object ScaleText:

    def fromDomain(scaleText: Question.ScaleText): QuestionFormat.ScaleText =
      QuestionFormat.ScaleText(
        min_bound_caption = scaleText.minBoundCaption,
        max_bound_caption = scaleText.maxBoundCaption
      )
