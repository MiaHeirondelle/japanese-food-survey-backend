package jp.ac.tachibana.food_survey.http.model.session.websocket

import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.websocket.WebSocketFrame

import jp.ac.tachibana.food_survey.http.model.session.SessionFormat
import jp.ac.tachibana.food_survey.http.model.user.UserFormat
import jp.ac.tachibana.food_survey.services.session.model.*

sealed trait OutputSessionMessageFormat:
  def messageType: OutputSessionMessageTypeFormat

object OutputSessionMessageFormat:

  implicit val encoder: Encoder[OutputSessionMessageFormat] =
    Encoder.AsObject.instance { r =>
      val base = r match {
        case rj: OutputSessionMessageFormat.UserJoined =>
          Encoder.AsObject[OutputSessionMessageFormat.UserJoined].encodeObject(rj)

        case sb: OutputSessionMessageFormat.SessionBegan =>
          Encoder.AsObject[OutputSessionMessageFormat.SessionBegan].encodeObject(sb)
      }
      base.add("type", Encoder[OutputSessionMessageTypeFormat].apply(r.messageType))
    }

  case class UserJoined(
    user: UserFormat,
    session: SessionFormat)
      extends OutputSessionMessageFormat
      derives Encoder.AsObject:
    val messageType: OutputSessionMessageTypeFormat =
      OutputSessionMessageTypeFormat.RespondentJoined

  case class SessionBegan(sessionFormat: SessionFormat) extends OutputSessionMessageFormat derives Encoder.AsObject:
    val messageType: OutputSessionMessageTypeFormat =
      OutputSessionMessageTypeFormat.SessionBegan

  def toWebSocketFrame(message: OutputSessionMessage): WebSocketFrame =
    message match {
      case OutputSessionMessage.UserJoined(user, session) =>
        jsonToSocketFrame(
          OutputSessionMessageFormat.UserJoined(
            UserFormat.fromDomain(user),
            SessionFormat.fromDomain(session)
          )
        )

      case OutputSessionMessage.SessionBegan(session) =>
        jsonToSocketFrame(
          OutputSessionMessageFormat.SessionBegan(
            SessionFormat.fromDomain(session)
          ))

      case OutputSessionMessage.Shutdown =>
        WebSocketFrame.Close()
    }

  private def jsonToSocketFrame(format: OutputSessionMessageFormat): WebSocketFrame =
    WebSocketFrame.Text(format.asJson.noSpaces)
