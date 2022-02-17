package jp.ac.tachibana.food_survey.persistence.session

import jp.ac.tachibana.food_survey.domain.session.Session

trait SessionRepository[F[_]]:

  def getActiveSession: F[Option[Session]]

  // todo: session already in progress error
  def createNewSession(session: Session.AwaitingUsers): F[Unit]

  // todo: no active session error
  def updateActiveSession(session: Session): F[Unit]
