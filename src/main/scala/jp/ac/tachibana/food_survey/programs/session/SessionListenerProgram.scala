package jp.ac.tachibana.food_survey.programs.session

import cats.data.NonEmptyList

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.programs.session.SessionListenerProgram.*

trait SessionListenerProgram[F[_]]:

  def getActiveSession: F[Option[Session]]

  def create[L](
    listenerBuilder: ListenerBuilder[F, L]
  )(creator: User.Admin,
    respondents: NonEmptyList[User.Id]): F[Either[SessionProgram.SessionCreationError, L]]

  def join[L](
    listenerBuilder: ListenerBuilder[F, L]
  )(respondent: User.Respondent): F[Either[SessionProgram.SessionJoinError, L]]

  // todo: update signature
  def stop: F[Unit]

object SessionListenerProgram:

  type ListenerInput[F[_]] =
    fs2.Stream[F, SessionListenerProgram.InputMessage]

  type ListenerOutput[F[_]] =
    fs2.Stream[F, SessionListenerProgram.OutputMessage]

  type ListenerInputTransformer[F[_]] =
    ListenerInput[F] => fs2.Stream[F, Unit]

  type ListenerBuilder[F[_], L] =
    (ListenerInputTransformer[F], ListenerOutput[F]) => F[L]

  sealed trait InputMessage

  object InputMessage:
    case class BeginSession(sessionNumber: Session.Number) extends SessionListenerProgram.InputMessage

  sealed trait OutputMessage

  object OutputMessage:
    case class RespondentJoined(
      user: User.Respondent,
      session: Session)
        extends SessionListenerProgram.OutputMessage

    case object Shutdown extends SessionListenerProgram.OutputMessage
