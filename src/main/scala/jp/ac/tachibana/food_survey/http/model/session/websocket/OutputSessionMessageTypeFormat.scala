package jp.ac.tachibana.food_survey.http.model.session.websocket

import io.circe.Encoder

enum OutputSessionMessageTypeFormat:
  case UserJoined, SessionBegan, TimerTick, TransitionToNextElement, ElementSelected, SessionFinished

object OutputSessionMessageTypeFormat:

  implicit val encoder: Encoder[OutputSessionMessageTypeFormat] =
    Encoder.encodeString.contramap {
      case OutputSessionMessageTypeFormat.UserJoined =>
        "user_joined"
      case OutputSessionMessageTypeFormat.SessionBegan =>
        "session_began"
      case OutputSessionMessageTypeFormat.TimerTick =>
        "timer_tick"
      case OutputSessionMessageTypeFormat.TransitionToNextElement =>
        "transition_to_next_element"
      case OutputSessionMessageTypeFormat.ElementSelected =>
        "element_selected"
      case OutputSessionMessageTypeFormat.SessionFinished =>
        "session_finished"
    }
