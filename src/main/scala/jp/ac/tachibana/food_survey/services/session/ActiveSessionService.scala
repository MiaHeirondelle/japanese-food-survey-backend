package jp.ac.tachibana.food_survey.services.session

import jp.ac.tachibana.food_survey.domain.question.QuestionAnswer
import jp.ac.tachibana.food_survey.domain.session.{Session, SessionElement}

trait ActiveSessionService[F[_]]:

  def registerSession(session: Session.InProgress): F[Unit]

  /** @return
    *   `None` if there are no elements left to provide. Returns the first element until the first transition is made.
    */
  def currentEntry(
    sessionNumber: Session.Number): F[Either[ActiveSessionService.Error, Option[ActiveSessionService.SessionElementState]]]

  def provideAnswer(
    questionAnswer: QuestionAnswer): F[Either[ActiveSessionService.Error, ActiveSessionService.SessionElementState.Question]]

  /** @return
    *   `None` if there are no elements left to provide. Otherwise, returns the next elements.
    */
  def transitionToNextElement(
    sessionNumber: Session.Number): F[Either[ActiveSessionService.Error, Option[ActiveSessionService.SessionElementState]]]

  def unregisterSession(sessionNumber: Session.Number): F[Unit]

object ActiveSessionService:

  sealed trait Error

  object Error:
    case object NoSessionInProgress extends ActiveSessionService.Error
    case object IncorrectSessionState extends ActiveSessionService.Error

  sealed trait SessionElementState

  object SessionElementState:
    case class Question(
      state: ActiveSessionService.QuestionState,
      question: SessionElement.Question)
        extends SessionElementState

  enum QuestionState:
    case Pending, Finished
