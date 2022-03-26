package jp.ac.tachibana.food_survey.services.session.managers

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User

trait AwaitingUsersSessionManager[F[_]]:

  def registerSession(session: Session.AwaitingUsers): F[Unit]

  def join(respondent: User.Respondent): F[Either[AwaitingUsersSessionManager.Error, Session.NotBegan]]

  def getCurrentState: F[Option[Session.NotBegan]]

  def unregisterSession: F[Unit]

object AwaitingUsersSessionManager:

  sealed trait Error

  object Error:
    case object InvalidSessionState extends AwaitingUsersSessionManager.Error
    case object InvalidParticipant extends AwaitingUsersSessionManager.Error
