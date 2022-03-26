package jp.ac.tachibana.food_survey.persistence.session

import jp.ac.tachibana.food_survey.domain.session.Session

trait SessionRepository[F[_]]:

  def getLatestSessionNumber: F[Option[Session.Number]]

  def getActiveSession: F[Option[Session.NotFinished]]

  // todo: session already in progress error
  def createNewSession(session: Session.AwaitingUsers): F[Unit]

  // todo: no active session error
  def setActiveSession(session: Session): F[Unit]

  // todo: no active session error
  def updateInProgressSession(update: Session.InProgress => Session.InProgressOrFinished): F[Option[Session.InProgressOrFinished]]

  // todo: remove
  def reset: F[Unit]
