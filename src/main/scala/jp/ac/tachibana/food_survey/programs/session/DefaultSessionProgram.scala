package jp.ac.tachibana.food_survey.programs.session

import cats.Monad
import cats.data.NonEmptyList
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.services.session.SessionService
import jp.ac.tachibana.food_survey.services.event_log.EventLogService
import cats.syntax.traverse.*
import cats.instances.either.*

class DefaultSessionProgram[F[_]: Monad](
  sessionService: SessionService[F],
  eventLogService: EventLogService[F])
    extends SessionProgram[F]:

  override def getActiveSession(forUserId: User.Id): F[Option[Session.NotFinished]] =
    sessionService.getActiveSession.map(_.map {
      case s: Session.InProgress =>
        s.withSortedRespondents(forUserId)
      case s =>
        s
    })

  override def create(
    creator: User.Admin,
    respondents: NonEmptyList[User.Id]): F[Either[SessionProgram.SessionCreationError, Session.AwaitingUsers]] =
    sessionService
      .create(creator, respondents)
      .flatTap(_.traverse(s => eventLogService.sessionCreate(s.number)))
      .map(_.left.map {
        case SessionService.CreateSessionError.InvalidParticipants =>
          SessionProgram.SessionCreationError.InvalidParticipants
        case SessionService.CreateSessionError.WrongSessionStatus =>
          SessionProgram.SessionCreationError.WrongSessionStatus
      })

  override def join(respondent: User.Respondent): F[Either[SessionProgram.SessionJoinError, Session.NotBegan]] =
    sessionService
      .join(respondent)
      .flatTap(_.traverse(s => eventLogService.sessionJoin(s.number, respondent.id)))
      .map(_.left.map {
        case SessionService.JoinSessionError.InvalidParticipant =>
          SessionProgram.SessionJoinError.InvalidParticipant
        case SessionService.JoinSessionError.WrongSessionStatus =>
          SessionProgram.SessionJoinError.WrongSessionStatus
      })

  override def stop: F[Unit] =
    sessionService.stop
