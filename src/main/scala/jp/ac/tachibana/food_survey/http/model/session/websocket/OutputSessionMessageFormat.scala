package jp.ac.tachibana.food_survey.http.model.session.websocket

import cats.syntax.option.*
import io.circe.Encoder
import io.circe.syntax.*
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

        case sb: OutputSessionMessageFormat.SessionBegan =>
          Encoder.AsObject[OutputSessionMessageFormat.SessionBegan].encodeObject(sb)
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

  case class SessionBegan(sessionFormat: SessionFormat) extends OutputSessionMessageFormat derives Encoder.AsObject:
    val messageType: OutputSessionMessageTypeFormat =
      OutputSessionMessageTypeFormat.SessionBegan

  def toWebSocketFrame(message: SessionListenerProgram.OutputMessage): Option[WebSocketFrame] =
    message match {
      case SessionListenerProgram.OutputMessage.RespondentJoined(user, session) =>
        jsonToSocketFrame(
          OutputSessionMessageFormat.RespondentJoined(
            UserFormat.fromDomain(user),
            SessionFormat.fromDomain(session)
          )
        ).some

      case SessionListenerProgram.OutputMessage.SessionBegan(session) =>
        jsonToSocketFrame(
          OutputSessionMessageFormat.SessionBegan(
            SessionFormat.fromDomain(session)
          )).some

      case SessionListenerProgram.OutputMessage.Shutdown =>
        WebSocketFrame.Close(1000).toOption
    }

  private def jsonToSocketFrame(format: OutputSessionMessageFormat): WebSocketFrame =
    WebSocketFrame.Text(format.asJson.noSpaces)
