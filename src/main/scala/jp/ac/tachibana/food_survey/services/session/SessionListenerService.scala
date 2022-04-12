package jp.ac.tachibana.food_survey.services.session

import cats.data.NonEmptyList
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.services.session.model.*

import scala.concurrent.duration.{Duration, FiniteDuration}

trait SessionListenerService[F[_]]:

  // todo: errors
  /** Connect to active session.
    */
  def connect[L](
    listenerBuilder: SessionListenerBuilder[F, L],
    processor: SessionMessageProcessor[F]
  )(user: User): F[Either[SessionListenerService.ConnectionError, L]]

  // todo: errors
  def broadcast(message: OutputSessionMessage): F[Unit]

  // todo: errors, tick id?
  def tickBroadcast(
    tick: FiniteDuration,
    limit: Duration
  )(createMessage: Duration => OutputSessionMessage): F[Unit]

  // todo:
  def stopTicks: F[Unit]

  // todo: update signature
  def stop: F[Unit]

object SessionListenerService:

  sealed trait ConnectionError

  object ConnectionError:

    case object InvalidSessionState extends SessionListenerService.ConnectionError
