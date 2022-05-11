package jp.ac.tachibana.food_survey.http.model.session.websocket

import cats.syntax.either.*
import cats.syntax.show.*
import io.circe.Decoder

enum InputSessionMessageTypeFormat:
  case BeginSession, ReadyToProceed, PauseSession, ResumeSession, ProvideIntermediateAnswer, ProvideAnswer

object InputSessionMessageTypeFormat:

  implicit val decoder: Decoder[InputSessionMessageTypeFormat] =
    Decoder.decodeString.emap {
      case "begin_session"               => InputSessionMessageTypeFormat.BeginSession.asRight
      case "ready_to_proceed"            => InputSessionMessageTypeFormat.ReadyToProceed.asRight
      case "pause_session"               => InputSessionMessageTypeFormat.PauseSession.asRight
      case "resume_session"              => InputSessionMessageTypeFormat.ResumeSession.asRight
      case "provide_intermediate_answer" => InputSessionMessageTypeFormat.ProvideIntermediateAnswer.asRight
      case "provide_answer"              => InputSessionMessageTypeFormat.ProvideAnswer.asRight
      case messageType                   => show"Unknown input session message type $messageType".asLeft
    }
