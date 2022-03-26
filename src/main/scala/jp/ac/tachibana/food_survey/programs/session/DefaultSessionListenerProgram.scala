package jp.ac.tachibana.food_survey.programs.session

import cats.Monad
import cats.data.{EitherT, NonEmptyList, OptionT}
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.services.session.model.*
import jp.ac.tachibana.food_survey.services.session.{SessionListenerService, SessionService}

class DefaultSessionListenerProgram[F[_]: Monad](
  sessionService: SessionService[F],
  sessionListenerService: SessionListenerService[F])
    extends SessionListenerProgram[F]:

  override def connect[L](
    listenerBuilder: SessionListenerBuilder[F, L]
  )(user: User): F[Either[SessionListenerProgram.ConnectionError, L]] =
    sessionListenerService
      .connect(listenerBuilder, processMessage)(user)
      .map(_.left.map { case SessionListenerService.ConnectionError.InvalidSessionState =>
        SessionListenerProgram.ConnectionError.InvalidSessionState
      })

  override def stop: F[Unit] =
    sessionListenerService.stop

  private def processMessage(
    message: InputSessionMessage,
    session: Session,
    user: User): F[Option[OutputSessionMessage]] =
    message match {
      case InputSessionMessage.BeginSession =>
        val beginF = for {
          session <- OptionT(sessionService.getActiveSession)
          beganSession <- OptionT(sessionService.begin(session.admin).map(_.toOption))
        } yield OutputSessionMessage.SessionBegan(beganSession): OutputSessionMessage
        beginF.value
      case InputSessionMessage.ReadyForNextQuestion =>
        ???
    }
