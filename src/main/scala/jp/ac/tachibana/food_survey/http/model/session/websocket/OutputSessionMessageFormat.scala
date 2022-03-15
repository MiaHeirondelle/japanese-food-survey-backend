package jp.ac.tachibana.food_survey.http.model.session.websocket

import io.circe.Encoder
import org.http4s.websocket.WebSocketFrame

import jp.ac.tachibana.food_survey.http.model.session.SessionFormat
import jp.ac.tachibana.food_survey.http.model.user.UserFormat
import jp.ac.tachibana.food_survey.programs.session.SessionListenerProgram
import jp.ac.tachibana.food_survey.programs.session.SessionListenerProgram.OutputMessage

sealed trait OutputSessionMessageFormat:
  def messageType: OutputSessionMessageTypeFormat

object OutputSessionMessageFormat:

  implicit val encoder: Encoder[OutputSessionMessageFormat] =
    Encoder.AsObject.instance { r =>
      val base = r match {
        case rj: OutputSessionMessageFormat.RespondentJoined =>
          Encoder.AsObject[OutputSessionMessageFormat.RespondentJoined].encodeObject(rj)
      }
      base.add("type", Encoder[OutputSessionMessageTypeFormat].apply(r.messageType))
    }

  case class RespondentJoined(
    user: UserFormat,
    session: SessionFormat)
      extends OutputSessionMessageFormat
      derives Encoder.AsObject:
    val messageType: OutputSessionMessageTypeFormat =
      OutputSessionMessageTypeFormat.RespondentJoined

  def toWebSocketFrame(message: SessionListenerProgram.OutputMessage): WebSocketFrame =
    message match {
      case SessionListenerProgram.OutputMessage.RespondentJoined(user, session) => ???

      case SessionListenerProgram.OutputMessage.Shutdown => ???
    }
