package jp.ac.tachibana.food_survey.http.model.session.websocket

import io.circe.Encoder

enum OutputSessionMessageTypeFormat:
  case UserJoined, SessionBegan, SessionPaused, TimerTick, BasicQuestionSelected, RepeatedQuestionSelected,
  BasicQuestionReviewSelected, RepeatedQuestionReviewSelected, TextSelected, SessionFinished

object OutputSessionMessageTypeFormat:

  implicit val encoder: Encoder[OutputSessionMessageTypeFormat] =
    Encoder.encodeString.contramap {
      case OutputSessionMessageTypeFormat.UserJoined =>
        "user_joined"
      case OutputSessionMessageTypeFormat.SessionBegan =>
        "session_began"
      case OutputSessionMessageTypeFormat.SessionPaused =>
        "session_paused"
      case OutputSessionMessageTypeFormat.TimerTick =>
        "timer_tick"
      case OutputSessionMessageTypeFormat.BasicQuestionSelected =>
        "basic_question_selected"
      case OutputSessionMessageTypeFormat.RepeatedQuestionSelected =>
        "repeated_question_selected"
      case OutputSessionMessageTypeFormat.BasicQuestionReviewSelected =>
        "basic_question_review_selected"
      case OutputSessionMessageTypeFormat.RepeatedQuestionReviewSelected =>
        "repeated_question_review_selected"
      case OutputSessionMessageTypeFormat.TextSelected =>
        "text_selected"
      case OutputSessionMessageTypeFormat.SessionFinished =>
        "session_finished"
    }
