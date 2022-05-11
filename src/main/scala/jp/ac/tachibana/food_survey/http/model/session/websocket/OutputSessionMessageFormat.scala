package jp.ac.tachibana.food_survey.http.model.session.websocket

import io.circe.{Encoder, JsonObject}
import io.circe.syntax.*
import org.http4s.websocket.WebSocketFrame
import jp.ac.tachibana.food_survey.http.model.question.{QuestionAnswerFormat, QuestionFormat}
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

        case OutputSessionMessageFormat.SessionPaused =>
          JsonObject.empty

        case tt: OutputSessionMessageFormat.TimerTick =>
          Encoder.AsObject[OutputSessionMessageFormat.TimerTick].encodeObject(tt)

        case bqs: OutputSessionMessageFormat.BasicQuestionSelected =>
          Encoder.AsObject[OutputSessionMessageFormat.BasicQuestionSelected].encodeObject(bqs)

        case rqs: OutputSessionMessageFormat.RepeatedQuestionSelected =>
          Encoder.AsObject[OutputSessionMessageFormat.RepeatedQuestionSelected].encodeObject(rqs)

        case bqr: OutputSessionMessageFormat.BasicQuestionReviewSelected =>
          Encoder.AsObject[OutputSessionMessageFormat.BasicQuestionReviewSelected].encodeObject(bqr)

        case bqr: OutputSessionMessageFormat.RepeatedQuestionReviewSelected =>
          Encoder.AsObject[OutputSessionMessageFormat.RepeatedQuestionReviewSelected].encodeObject(bqr)

        case t: OutputSessionMessageFormat.TextSelected =>
          Encoder.AsObject[OutputSessionMessageFormat.TextSelected].encodeObject(t)

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

  case object SessionPaused extends OutputSessionMessageFormat(OutputSessionMessageTypeFormat.SessionPaused)
      derives Encoder.AsObject

  case class TimerTick(time_left_in_ms: Long) extends OutputSessionMessageFormat(OutputSessionMessageTypeFormat.TimerTick)
      derives Encoder.AsObject

  case class BasicQuestionSelected(
    element: SessionElementFormat)
      extends OutputSessionMessageFormat(OutputSessionMessageTypeFormat.BasicQuestionSelected)
      derives Encoder.AsObject

  case class RepeatedQuestionSelected(
    element: SessionElementFormat,
    previous_answers: List[QuestionAnswerFormat])
      extends OutputSessionMessageFormat(OutputSessionMessageTypeFormat.RepeatedQuestionSelected)
      derives Encoder.AsObject

  case class BasicQuestionReviewSelected(
    element: SessionElementFormat,
    answers: List[QuestionAnswerFormat])
      extends OutputSessionMessageFormat(OutputSessionMessageTypeFormat.BasicQuestionReviewSelected)
      derives Encoder.AsObject

  case class RepeatedQuestionReviewSelected(
    element: SessionElementFormat,
    answers: List[QuestionAnswerFormat],
    previous_answers: List[QuestionAnswerFormat])
      extends OutputSessionMessageFormat(OutputSessionMessageTypeFormat.RepeatedQuestionReviewSelected)
      derives Encoder.AsObject

  case class TextSelected(element: SessionElementFormat)
      extends OutputSessionMessageFormat(OutputSessionMessageTypeFormat.TextSelected) derives Encoder.AsObject

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

      case OutputSessionMessage.SessionPaused =>
        jsonToSocketFrame(OutputSessionMessageFormat.SessionPaused)

      case OutputSessionMessage.TimerTick(remainingTimeMs) =>
        jsonToSocketFrame(
          OutputSessionMessageFormat.TimerTick(remainingTimeMs)
        )

      case OutputSessionMessage.BasicQuestionSelected(element) =>
        jsonToSocketFrame(
          OutputSessionMessageFormat.BasicQuestionSelected(
            SessionElementFormat.fromDomain(element)
          )
        )

      case OutputSessionMessage.RepeatedQuestionSelected(element, previousAnswers) =>
        jsonToSocketFrame(
          OutputSessionMessageFormat.RepeatedQuestionSelected(
            SessionElementFormat.fromDomain(element),
            previousAnswers.map(QuestionAnswerFormat.fromDomain)
          )
        )

      case OutputSessionMessage.BasicQuestionReviewSelected(element, answers) =>
        jsonToSocketFrame(
          OutputSessionMessageFormat.BasicQuestionReviewSelected(
            SessionElementFormat.fromDomain(element),
            answers.map(QuestionAnswerFormat.fromDomain)
          )
        )

      case OutputSessionMessage.RepeatedQuestionReviewSelected(element, answers, previousAnswers) =>
        jsonToSocketFrame(
          OutputSessionMessageFormat.RepeatedQuestionReviewSelected(
            SessionElementFormat.fromDomain(element),
            answers.map(QuestionAnswerFormat.fromDomain),
            previousAnswers.map(QuestionAnswerFormat.fromDomain)
          )
        )

      case OutputSessionMessage.TextSelected(element) =>
        jsonToSocketFrame(
          OutputSessionMessageFormat.TextSelected(
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
