package jp.ac.tachibana.food_survey.http.model.session.websocket

import io.circe.{Encoder, JsonObject}
import io.circe.syntax.*
import org.http4s.websocket.WebSocketFrame
import jp.ac.tachibana.food_survey.http.model.question.QuestionFormat
import jp.ac.tachibana.food_survey.http.model.session.{SessionElementFormat, SessionFormat}
import jp.ac.tachibana.food_survey.http.model.user.UserFormat
import jp.ac.tachibana.food_survey.services.session.model.*

sealed abstract class OutputSessionMessageFormat(val `type`: OutputSessionMessageTypeFormat)

object OutputSessionMessageFormat:

  implicit val encoder: Encoder[OutputSessionMessageFormat] =
    Encoder.AsObject.instance { r =>
      val base = r match {
        case rj: OutputSessionMessageFormat.UserJoined =>
          Encoder.AsObject[OutputSessionMessageFormat.UserJoined].encodeObject(rj)

        case sb: OutputSessionMessageFormat.SessionBegan =>
          Encoder.AsObject[OutputSessionMessageFormat.SessionBegan].encodeObject(sb)

        case tt: OutputSessionMessageFormat.TimerTick =>
          Encoder.AsObject[OutputSessionMessageFormat.TimerTick].encodeObject(tt)

        case OutputSessionMessageFormat.TransitionToNextElement =>
          JsonObject.empty

        case qs: OutputSessionMessageFormat.ElementSelected =>
          Encoder.AsObject[OutputSessionMessageFormat.ElementSelected].encodeObject(qs)

        case sf: OutputSessionMessageFormat.SessionFinished =>
          Encoder.AsObject[OutputSessionMessageFormat.SessionFinished].encodeObject(sf)
      }
      base.add("type", Encoder[OutputSessionMessageTypeFormat].apply(r.`type`))
    }

  case class UserJoined(
    user: UserFormat,
    session: SessionFormat)
      extends OutputSessionMessageFormat(OutputSessionMessageTypeFormat.UserJoined)
      derives Encoder.AsObject

  case class SessionBegan(session: SessionFormat) extends OutputSessionMessageFormat(OutputSessionMessageTypeFormat.SessionBegan)
      derives Encoder.AsObject

  case class TimerTick(time_left_in_ms: Long) extends OutputSessionMessageFormat(OutputSessionMessageTypeFormat.TimerTick)
      derives Encoder.AsObject

  case object TransitionToNextElement extends OutputSessionMessageFormat(OutputSessionMessageTypeFormat.TransitionToNextElement)

  case class ElementSelected(
    session: SessionFormat,
    element: SessionElementFormat)
      extends OutputSessionMessageFormat(OutputSessionMessageTypeFormat.ElementSelected)
      derives Encoder.AsObject

  case class SessionFinished(
    session: SessionFormat)
      extends OutputSessionMessageFormat(OutputSessionMessageTypeFormat.SessionFinished)
      derives Encoder.AsObject

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

      case OutputSessionMessage.TimerTick(remainingTimeMs) =>
        jsonToSocketFrame(
          OutputSessionMessageFormat.TimerTick(remainingTimeMs)
        )

      case OutputSessionMessage.TransitionToNextElement =>
        jsonToSocketFrame(
          OutputSessionMessageFormat.TransitionToNextElement
        )

      case OutputSessionMessage.ElementSelected(session, element) =>
        jsonToSocketFrame(
          OutputSessionMessageFormat.ElementSelected(
            SessionFormat.fromDomain(session),
            SessionElementFormat.fromDomain(element)
          )
        )

      case OutputSessionMessage.SessionFinished(session) =>
        jsonToSocketFrame(
          OutputSessionMessageFormat.SessionFinished(
            SessionFormat.fromDomain(session)
          )
        )

      case OutputSessionMessage.Shutdown =>
        WebSocketFrame.Close()
    }

  private def jsonToSocketFrame(format: OutputSessionMessageFormat): WebSocketFrame =
    WebSocketFrame.Text(format.asJson.noSpaces)
