package jp.ac.tachibana.food_survey.http.model.session

import io.circe.Encoder

enum SessionStatusFormat:
  case NotCreated, AwaitingUsers, CanBegin, InProgress

object SessionStatusFormat:

  implicit val sessionStatusEncoder: Encoder[SessionStatusFormat] =
    Encoder.encodeString.contramap {
      case SessionStatusFormat.NotCreated =>
        "not_created"
      case SessionStatusFormat.AwaitingUsers =>
        "awaiting_users"
      case SessionStatusFormat.CanBegin =>
        "can_begin"
      case SessionStatusFormat.InProgress =>
        "in_progress"
    }
