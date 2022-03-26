package jp.ac.tachibana.food_survey.services.session

import cats.data.NonEmptyList

import jp.ac.tachibana.food_survey.domain.question.QuestionAnswer
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User

trait SessionService[F[_]]:

  def getActiveSession: F[Option[Session.NotFinished]]

  def create(
    creator: User.Admin,
    respondents: NonEmptyList[User.Id]): F[Either[SessionService.SessionCreationError, Session.AwaitingUsers]]

  def join(respondent: User.Respondent): F[Either[SessionService.SessionJoinError, Session.NotBegan]]

  def begin(admin: User.Admin): F[Either[SessionService.SessionBeginError, Session.InProgress]]

  // todo: wrong answer error
  def provideAnswer(answer: QuestionAnswer): F[Either[SessionService.ProvideAnswerError, Session.InProgressOrFinished]]

  def finish: F[Either[SessionService.SessionFinishError, Session.Finished]]

  // todo: update signature
  def stop: F[Unit]

object SessionService:

  sealed trait SessionCreationError

  object SessionCreationError:
    case object InvalidParticipants extends SessionCreationError
    case object WrongSessionStatus extends SessionCreationError

  sealed trait SessionJoinError

  object SessionJoinError:
    case object InvalidParticipant extends SessionJoinError
    case object WrongSessionStatus extends SessionJoinError

  sealed trait SessionBeginError

  object SessionBeginError:
    case object WrongSessionStatus extends SessionBeginError

  sealed trait ProvideAnswerError

  object ProvideAnswerError:
    case object WrongSessionStatus extends ProvideAnswerError

  sealed trait SessionFinishError

  object SessionFinishError:
    case object WrongSessionStatus extends SessionFinishError
