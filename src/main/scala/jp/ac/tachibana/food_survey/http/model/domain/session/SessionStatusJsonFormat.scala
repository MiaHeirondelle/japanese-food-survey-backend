package jp.ac.tachibana.food_survey.http.model.domain.session

import io.circe.Encoder

enum SessionStatusJsonFormat:
  case NotCreated, AwaitingUsers, CanBegin, InProgress

object SessionStatusJsonFormat:

  implicit val sessionStatusEncoder: Encoder[SessionStatusJsonFormat] =
    Encoder.encodeString.contramap {
      case SessionStatusJsonFormat.NotCreated =>
        "not_created"
      case SessionStatusJsonFormat.AwaitingUsers =>
        "awaiting_users"
      case SessionStatusJsonFormat.CanBegin =>
        "can_begin"
      case SessionStatusJsonFormat.InProgress =>
        "in_progress"
    }
