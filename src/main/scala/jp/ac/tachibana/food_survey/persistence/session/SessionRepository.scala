package jp.ac.tachibana.food_survey.persistence.session

import jp.ac.tachibana.food_survey.domain.session.Session

trait SessionRepository[F[_]]:

  def getLatestSessionNumber: F[Option[Session.Number]]

  def getActiveSession: F[Option[Session.AwaitingUsers]]

  // todo: session already in progress error
  def createNewSession(session: Session.AwaitingUsers): F[Unit]

  // todo: session doesn't exist/already finished error
  def finishSession(session: Session.Finished): F[Unit]

  // todo: remove
  def reset: F[Unit]
