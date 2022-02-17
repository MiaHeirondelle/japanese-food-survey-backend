package jp.ac.tachibana.food_survey.programs.session

import cats.data.NonEmptyList

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User

trait SessionProgram[F[_]]:

  def getActiveSession: F[Option[Session]]

  def create(
    creator: User.Admin,
    respondents: NonEmptyList[User.Id]): F[Either[SessionProgram.SessionCreationError, Session.AwaitingUsers]]

object SessionProgram:

  sealed trait SessionCreationError

  object SessionCreationError:

    case object WrongSessionStatus extends SessionCreationError
    case object InvalidParticipants extends SessionCreationError
