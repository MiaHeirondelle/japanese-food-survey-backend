package jp.ac.tachibana.food_survey.http.model.session.websocket

import cats.syntax.either.*
import cats.syntax.show.*
import io.circe.Decoder

enum InputSessionMessageTypeFormat:
  case BeginSession

object InputSessionMessageTypeFormat:

  implicit val decoder: Decoder[InputSessionMessageTypeFormat] =
    Decoder.decodeString.emap {
      case "begin_session" => InputSessionMessageTypeFormat.BeginSession.asRight
      case messageType     => show"Unknown input session message type $messageType".asLeft
    }
