package jp.ac.tachibana.food_survey.services.session

import cats.data.NonEmptyList

import jp.ac.tachibana.food_survey.domain.question.QuestionAnswer
import jp.ac.tachibana.food_survey.domain.session.{Session, SessionElement}
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.domain.user.User.Respondent

// todo: restructure to session state service and session operations service
trait SessionService[F[_]]:

  def getActiveSession: F[Option[Session.NotFinished]]

  def create(
    creator: User.Admin,
    respondents: NonEmptyList[User.Id]): F[Either[SessionService.CreateSessionError, Session.AwaitingUsers]]

  def join(respondent: User.Respondent): F[Either[SessionService.JoinSessionError, Session.NotBegan]]

  def begin(admin: User.Admin): F[Either[SessionService.BeginSessionError, Session.InProgress]]

  def pause: F[Either[SessionService.PauseSessionError, SessionService.SessionElementState.Paused]]

  def resume: F[Either[SessionService.ResumeSessionError, SessionService.NonPendingSessionElementState]]

  def getCurrentElementState: F[Either[SessionService.GetCurrentElementStateError, SessionService.SessionElementState]]

  def provideAnswer(
    answer: QuestionAnswer): F[Either[SessionService.ProvideAnswerError, SessionService.SessionElementState.Question]]

  def transitionToNextElement
    : F[Either[SessionService.TransitionToNextElementError, SessionService.NonPendingSessionElementState]]

  def transitionToFirstElement(
    respondentId: User.Id): F[Either[SessionService.TransitionToNextElementError, SessionService.SessionElementState]]

  def finish: F[Either[SessionService.FinishSessionError, Session.Finished]]

  // todo: update signature
  def stop: F[Unit]

object SessionService:

  sealed trait SessionElementState:
    def session: Session.InProgressOrFinished

  sealed trait NonPendingSessionElementState extends SessionElementState

  object SessionElementState:

    case class BeforeFirstElement(session: Session.InProgress) extends SessionElementState
    case class Paused(pausedState: SessionService.NonPendingSessionElementState) extends SessionElementState {
      override def session: Session.InProgressOrFinished = pausedState.session
    }
    case class Question(
      session: Session.InProgress,
      state: SessionService.QuestionState,
      question: SessionElement.Question)
        extends NonPendingSessionElementState
    case class QuestionReview(
      session: Session.InProgress,
      questionReview: SessionElement.QuestionReview)
        extends NonPendingSessionElementState
    case class Text(
      session: Session.InProgress,
      text: SessionElement.Text)
        extends NonPendingSessionElementState
    case class Finished(session: Session.Finished) extends NonPendingSessionElementState

  enum QuestionState:
    case Pending, Finished

  sealed trait CreateSessionError

  object CreateSessionError:
    case object InvalidParticipants extends SessionService.CreateSessionError
    case object WrongSessionStatus extends SessionService.CreateSessionError

  sealed trait JoinSessionError

  object JoinSessionError:
    case object InvalidParticipant extends SessionService.JoinSessionError
    case object WrongSessionStatus extends SessionService.JoinSessionError

  sealed trait BeginSessionError

  object BeginSessionError:
    case object WrongSessionStatus extends SessionService.BeginSessionError
    case object InvalidTemplate extends SessionService.BeginSessionError

  sealed trait PauseSessionError

  object PauseSessionError:
    case object WrongSessionStatus extends SessionService.PauseSessionError

  sealed trait ResumeSessionError

  object ResumeSessionError:
    case object WrongSessionStatus extends SessionService.ResumeSessionError

  sealed trait GetCurrentElementStateError

  object GetCurrentElementStateError:
    case object IncorrectSessionState extends SessionService.GetCurrentElementStateError

  sealed trait TransitionToNextElementError

  object TransitionToNextElementError:
    case object WrongSessionStatus extends SessionService.TransitionToNextElementError

  sealed trait ProvideAnswerError

  object ProvideAnswerError:
    case object IncorrectSessionState extends SessionService.ProvideAnswerError

  sealed trait FinishSessionError

  object FinishSessionError:
    case object WrongSessionStatus extends SessionService.FinishSessionError
