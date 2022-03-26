package jp.ac.tachibana.food_survey.services.session.managers

import jp.ac.tachibana.food_survey.domain.question.QuestionAnswer
import jp.ac.tachibana.food_survey.domain.session.{Session, SessionElement}
import jp.ac.tachibana.food_survey.services.session.SessionService

trait InProgressSessionManager[F[_]]:

  def registerSession(session: Session.InProgress): F[Unit]

  def getCurrentState: F[Option[SessionService.SessionElementState]]

  def provideAnswer(
    answer: QuestionAnswer): F[Either[InProgressSessionManager.Error, SessionService.SessionElementState.Question]]

  /** @return
    *   `None` if there are no elements left to provide. Otherwise, returns the next elements.
    */
  def transitionToNextElement: F[Either[InProgressSessionManager.Error, Option[SessionService.SessionElementState]]]

  def unregisterSession: F[Unit]

object InProgressSessionManager:

  sealed trait Error

  object Error:
    case object IncorrectSessionState extends InProgressSessionManager.Error
