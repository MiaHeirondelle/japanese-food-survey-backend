package jp.ac.tachibana.food_survey.programs.session

import cats.data.NonEmptyList

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User

trait SessionProgram[F[_]]:

  def getActiveSession: F[Option[Session]]

  def create(
    creator: User.Admin,
    respondents: NonEmptyList[User.Id]): F[Either[SessionProgram.SessionCreationError, Session.AwaitingUsers]]

  def join(
    respondent: User.Respondent): F[Either[SessionProgram.SessionJoinError, Unit]]

  def begin(admin: User.Admin): F[Either[SessionProgram.SessionBeginError, Unit]]

  // todo: update signature
  def stop: F[Unit]

object SessionProgram:

  sealed trait SessionCreationError

  object SessionCreationError:

    case object WrongSessionStatus extends SessionCreationError
    case object InvalidParticipants extends SessionCreationError

  sealed trait SessionJoinError

  object SessionJoinError:

    case object WrongSessionStatus extends SessionJoinError
    case object InvalidParticipant extends SessionJoinError

  sealed trait SessionBeginError

  object SessionBeginError:
    case object WrongSessionStatus extends SessionBeginError
