package jp.ac.tachibana.food_survey.http.model.question

import io.circe.Encoder

enum QuestionTypeFormat:
  case Basic, Repeated

object QuestionTypeFormat:

  implicit val encoder: Encoder[QuestionTypeFormat] =
    Encoder.encodeString.contramap {
      case QuestionTypeFormat.Basic    => "basic"
      case QuestionTypeFormat.Repeated => "repeated"
    }
