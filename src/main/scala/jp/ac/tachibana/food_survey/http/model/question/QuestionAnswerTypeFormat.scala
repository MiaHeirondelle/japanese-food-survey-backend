package jp.ac.tachibana.food_survey.http.model.question

import io.circe.Encoder

enum QuestionAnswerTypeFormat:
  case Basic, Repeated

object QuestionAnswerTypeFormat:

  implicit val encoder: Encoder[QuestionAnswerTypeFormat] =
    Encoder.encodeString.contramap {
      case QuestionAnswerTypeFormat.Basic    => "basic"
      case QuestionAnswerTypeFormat.Repeated => "repeated"
    }
