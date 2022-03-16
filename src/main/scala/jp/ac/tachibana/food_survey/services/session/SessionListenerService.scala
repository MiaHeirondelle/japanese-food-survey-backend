package jp.ac.tachibana.food_survey.services.session

import cats.data.NonEmptyList

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User

trait SessionListenerService[F[_]]:

  // todo: errors
  /** Connect to active session.
    */
  def connect[L](
    listenerBuilder: SessionListenerService.ListenerBuilder[F, L],
    processor: SessionListenerService.MessageProcessor[F]
  )(user: User): F[Either[SessionListenerService.ConnectionError, L]]

  // todo: update signature
  def stop: F[Unit]

object SessionListenerService:

  sealed trait ConnectionError

  object ConnectionError:

    case object InvalidSessionState extends SessionListenerService.ConnectionError

  type MessageProcessor[F[_]] =
    (SessionListenerService.InputMessage, Session, User) => F[Option[SessionListenerService.OutputMessage]]

  type ListenerInput[F[_]] =
    fs2.Stream[F, SessionListenerService.InputMessage]

  type ListenerOutput[F[_]] =
    fs2.Stream[F, SessionListenerService.OutputMessage]

  type ListenerInputTransformer[F[_]] =
    ListenerInput[F] => fs2.Stream[F, Unit]

  type ListenerBuilder[F[_], L] =
    (ListenerInputTransformer[F], ListenerOutput[F]) => F[L]

  sealed trait InputMessage

  object InputMessage:
    case object BeginSession extends SessionListenerService.InputMessage

  sealed trait OutputMessage

  object OutputMessage:
    case class UserJoined(
      user: User,
      session: Session)
        extends SessionListenerService.OutputMessage

    case class SessionBegan(session: Session.InProgress) extends SessionListenerService.OutputMessage

    case object Shutdown extends SessionListenerService.OutputMessage
