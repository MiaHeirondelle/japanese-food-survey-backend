package jp.ac.tachibana.food_survey.programs.session

import cats.Functor
import cats.data.NonEmptyList
import cats.syntax.functor.*

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.services.session.SessionService
import jp.ac.tachibana.food_survey.services.session.SessionService.SessionCreationError

class DefaultSessionProgram[F[_]: Functor](sessionService: SessionService[F]) extends SessionProgram[F]:

  override def getActiveSession: F[Option[Session.NotFinished]] =
    sessionService.getActiveSession

  override def create(
    creator: User.Admin,
    respondents: NonEmptyList[User.Id]): F[Either[SessionProgram.SessionCreationError, Session.AwaitingUsers]] =
    sessionService
      .create(creator, respondents)
      .map(_.left.map {
        case SessionService.SessionCreationError.InvalidParticipants =>
          SessionProgram.SessionCreationError.InvalidParticipants
        case SessionService.SessionCreationError.WrongSessionStatus =>
          SessionProgram.SessionCreationError.WrongSessionStatus
      })

  override def join(respondent: User.Respondent): F[Either[SessionProgram.SessionJoinError, Session.NotBegan]] =
    sessionService
      .join(respondent)
      .map(_.left.map {
        case SessionService.SessionJoinError.InvalidParticipant =>
          SessionProgram.SessionJoinError.InvalidParticipant
        case SessionService.SessionJoinError.WrongSessionStatus =>
          SessionProgram.SessionJoinError.WrongSessionStatus
      })

  override def stop: F[Unit] =
    sessionService.stop
