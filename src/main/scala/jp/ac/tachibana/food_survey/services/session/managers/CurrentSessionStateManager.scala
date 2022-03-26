package jp.ac.tachibana.food_survey.services.session.managers

import jp.ac.tachibana.food_survey.domain.session.Session

// todo: errors
trait CurrentSessionStateManager[F[_]]:

  def getLatestSessionNumber: F[Option[Session.Number]]

  def getCurrentSession: F[Option[Session]]

  def createNewSession(session: Session.AwaitingUsers): F[Unit]

  def registerInProgressSession(session: Session.InProgress): F[Unit]

  def finishSession(session: Session.Finished): F[Unit]

  def refreshAwaitingUsersSessionManager: F[Unit]

  def reset: F[Unit]
