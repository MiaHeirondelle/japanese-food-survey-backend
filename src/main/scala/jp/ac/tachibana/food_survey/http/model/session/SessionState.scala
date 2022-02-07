package jp.ac.tachibana.food_survey.http.model.session

import io.circe.Encoder

enum SessionState:
  case NotStarted, AwaitingUsers, CanBegin, InProgress, Finished

object SessionState:

  implicit val encoder: Encoder[SessionState] =
    Encoder.encodeString.contramap {
      case SessionState.NotStarted =>
        "not_started"
      case SessionState.AwaitingUsers =>
        "awaiting_users"
      case SessionState.CanBegin =>
        "can_begin"
      case SessionState.InProgress =>
        "in_progress"
      case SessionState.Finished =>
        "finished"
    }

