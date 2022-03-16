package jp.ac.tachibana.food_survey.services.session

import cats.data.NonEmptyList

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.services.session.model.*

trait SessionListenerService[F[_]]:

  // todo: errors
  /** Connect to active session.
    */
  def connect[L](
    listenerBuilder: SessionListenerBuilder[F, L],
    processor: SessionMessageProcessor[F]
  )(user: User): F[Either[SessionListenerService.ConnectionError, L]]

  // todo: update signature
  def stop: F[Unit]

object SessionListenerService:

  sealed trait ConnectionError

  object ConnectionError:

    case object InvalidSessionState extends SessionListenerService.ConnectionError
