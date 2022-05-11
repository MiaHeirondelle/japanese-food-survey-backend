package jp.ac.tachibana.food_survey.http.model.session.websocket

import cats.syntax.either.*
import cats.syntax.option.*
import io.circe.parser.decode
import io.circe.{Decoder, DecodingFailure}
import org.http4s.websocket.WebSocketFrame
import cats.syntax.functorFilter.*

import jp.ac.tachibana.food_survey.domain.question.{Question, QuestionAnswer}
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.services.session.model.*

sealed trait InputSessionMessageFormat

object InputSessionMessageFormat:

  implicit val decoder: Decoder[InputSessionMessageFormat] =
    Decoder.instance { cursor =>
      for {
        messageType <- cursor.downField("type").as[InputSessionMessageTypeFormat]
        result <- messageType match {
          case InputSessionMessageTypeFormat.BeginSession =>
            InputSessionMessageFormat.BeginSession.asRight

          case InputSessionMessageTypeFormat.ReadyToProceed =>
            InputSessionMessageFormat.ReadyToProceed.asRight

          case InputSessionMessageTypeFormat.ResumeSession =>
            InputSessionMessageFormat.ResumeSession.asRight

          case InputSessionMessageTypeFormat.PauseSession =>
            InputSessionMessageFormat.PauseSession.asRight

          case InputSessionMessageTypeFormat.ProvideIntermediateAnswer =>
            Decoder[InputSessionMessageFormat.ProvideIntermediateAnswer].apply(cursor)

          case InputSessionMessageTypeFormat.ProvideAnswer =>
            Decoder[InputSessionMessageFormat.ProvideAnswer].apply(cursor)
        }
      } yield result
    }

  case object BeginSession extends InputSessionMessageFormat
  case object ReadyToProceed extends InputSessionMessageFormat
  case object PauseSession extends InputSessionMessageFormat
  case object ResumeSession extends InputSessionMessageFormat
  case class ProvideIntermediateAnswer(
    question_id: String,
    scale_value: Option[Int],
    comment: Option[String])
      extends InputSessionMessageFormat
      derives Decoder

  case class ProvideAnswer(
    question_id: String,
    scale_value: Option[Int],
    comment: Option[String])
      extends InputSessionMessageFormat
      derives Decoder

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

        case InputSessionMessageFormat.ReadyToProceed =>
          InputSessionMessage.ReadyToProceed

        case InputSessionMessageFormat.PauseSession =>
          InputSessionMessage.PauseSession

        case InputSessionMessageFormat.ResumeSession =>
          InputSessionMessage.ResumeSession

        case InputSessionMessageFormat.ProvideIntermediateAnswer(questionId, scaleValue, comment) =>
          InputSessionMessage.ProvideIntermediateAnswer(
            Question.Id(questionId),
            scaleValue.map(QuestionAnswer.ScaleValue(_)),
            // todo: move validation
            comment.flatMap { c =>
              val trimmed = c.trim
              Option.when(trimmed.nonEmpty)(QuestionAnswer.Comment(trimmed))
            }
          )

        case InputSessionMessageFormat.ProvideAnswer(questionId, scaleValue, comment) =>
          InputSessionMessage.ProvideAnswer(
            Question.Id(questionId),
            scaleValue.map(QuestionAnswer.ScaleValue(_)),
            // todo: move validation
            comment.flatMap { c =>
              val trimmed = c.trim
              Option.when(trimmed.nonEmpty)(QuestionAnswer.Comment(trimmed))
            }
          )
      }
