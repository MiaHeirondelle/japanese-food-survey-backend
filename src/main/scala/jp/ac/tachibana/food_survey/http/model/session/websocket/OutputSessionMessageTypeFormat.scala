package jp.ac.tachibana.food_survey.http.model.session.websocket

import io.circe.Encoder

enum OutputSessionMessageTypeFormat:
  case RespondentJoined, SessionBegan

object OutputSessionMessageTypeFormat:

  implicit val encoder: Encoder[OutputSessionMessageTypeFormat] =
    Encoder.encodeString.contramap {
      case OutputSessionMessageTypeFormat.RespondentJoined =>
        "respondent_joined"
      case OutputSessionMessageTypeFormat.SessionBegan =>
        "session_began"
    }
