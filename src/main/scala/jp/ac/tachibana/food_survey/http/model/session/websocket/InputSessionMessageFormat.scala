package jp.ac.tachibana.food_survey.http.model.session.websocket

import cats.syntax.option.*
import io.circe.parser.decode
import io.circe.{Decoder, DecodingFailure}
import org.http4s.websocket.WebSocketFrame

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.programs.session.SessionListenerProgram

sealed trait InputSessionMessageFormat

object InputSessionMessageFormat:

  case class BeginSession(sessionNumber: Int) extends InputSessionMessageFormat derives Decoder

  implicit val decoder: Decoder[InputSessionMessageFormat] =
    Decoder.instance { cursor =>
      for {
        messageType <- cursor.downField("type").as[InputSessionMessageTypeFormat]
        result <- messageType match {
          case InputSessionMessageTypeFormat.BeginSession =>
            Decoder[InputSessionMessageFormat.BeginSession].apply(cursor)
        }
      } yield result
    }

  def fromWebSocketFrame(frame: WebSocketFrame): Option[SessionListenerProgram.InputMessage] =
    frame match {
      case WebSocketFrame.Text(text, _) =>
        decode[InputSessionMessageFormat](text).fold(_ => None, _.toDomain.some)
    }

  extension (format: InputSessionMessageFormat)
    def toDomain: SessionListenerProgram.InputMessage =
      format match {
        case InputSessionMessageFormat.BeginSession(number) =>
          SessionListenerProgram.InputMessage.BeginSession(Session.Number(number))
      }
