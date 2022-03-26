package jp.ac.tachibana.food_survey.http.model.session.websocket

import cats.syntax.either.*
import cats.syntax.option.*
import io.circe.parser.decode
import io.circe.{Decoder, DecodingFailure}
import org.http4s.websocket.WebSocketFrame

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.services.session.model.*

sealed trait InputSessionMessageFormat

object InputSessionMessageFormat:

  case object BeginSession extends InputSessionMessageFormat
  case object ReadyForNextQuestion extends InputSessionMessageFormat

  implicit val decoder: Decoder[InputSessionMessageFormat] =
    Decoder.instance { cursor =>
      for {
        messageType <- cursor.downField("type").as[InputSessionMessageTypeFormat]
        result <- messageType match {
          case InputSessionMessageTypeFormat.BeginSession =>
            InputSessionMessageFormat.BeginSession.asRight

          case InputSessionMessageTypeFormat.ReadyForNextQuestion =>
            InputSessionMessageFormat.ReadyForNextQuestion.asRight
        }
      } yield result
    }

  def fromWebSocketFrame(frame: WebSocketFrame): Option[InputSessionMessage] =
    frame match {
      case WebSocketFrame.Text(text, _) =>
        decode[InputSessionMessageFormat](text).fold(_ => None, _.toDomain.some)

      case _ =>
        None
    }

  extension (format: InputSessionMessageFormat)
    def toDomain: InputSessionMessage =
      format match {
        case InputSessionMessageFormat.BeginSession =>
          InputSessionMessage.BeginSession

        case InputSessionMessageFormat.ReadyForNextQuestion =>
          InputSessionMessage.ReadyForNextQuestion
      }
