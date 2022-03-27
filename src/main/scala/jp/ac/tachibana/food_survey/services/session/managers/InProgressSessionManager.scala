package jp.ac.tachibana.food_survey.services.session.managers

import jp.ac.tachibana.food_survey.domain.question.QuestionAnswer
import jp.ac.tachibana.food_survey.domain.session.{Session, SessionElement}
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.services.session.SessionService

trait InProgressSessionManager[F[_]]:

  private[managers] def registerSession(session: Session.InProgress): F[Unit]

  def getCurrentState: F[Option[SessionService.SessionElementState]]

  def provideAnswer(
    answer: QuestionAnswer): F[Either[InProgressSessionManager.Error, SessionService.SessionElementState.Question]]

  /** Immediately transitions to then ext element.
    */
  def transitionToNextElement: F[Either[InProgressSessionManager.Error, SessionService.NonPendingSessionElementState]]

  /** Will wait for all the respondents to call this method before transitioning.
    * @return
    *   `None` if there are no elements left to provide. Otherwise, returns the next element state.
    */
  def transitionToNextElement(
    respondentId: User.Id): F[Either[InProgressSessionManager.Error, SessionService.SessionElementState]]

  private[managers] def unregisterSession: F[Unit]

object InProgressSessionManager:

  sealed trait Error

  object Error:
    case object IncorrectSessionState extends InProgressSessionManager.Error
