package jp.ac.tachibana.food_survey.programs.session

import cats.data.NonEmptyList

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.services.session.model.*

trait SessionListenerProgram[F[_]]:

  def connect[L](listenerBuilder: SessionListenerBuilder[F, L])(user: User): F[Either[SessionListenerProgram.ConnectionError, L]]

  // todo: update signature
  def stop: F[Unit]

object SessionListenerProgram:

  sealed trait ConnectionError

  object ConnectionError:

    case object InvalidSessionState extends SessionListenerProgram.ConnectionError
