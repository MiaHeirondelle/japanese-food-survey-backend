package jp.ac.tachibana.food_survey.http.model.session

import io.circe.Encoder

enum SessionElementTypeFormat:
  case Question, QuestionReview

object SessionElementTypeFormat:

  implicit val encoder: Encoder[SessionElementTypeFormat] =
    Encoder.encodeString.contramap {
      case SessionElementTypeFormat.Question       => "question"
      case SessionElementTypeFormat.QuestionReview => "question_review"
    }
