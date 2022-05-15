package jp.ac.tachibana.food_survey.services.event_log

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User

trait EventLogService[F[_]]:

  def userLogin(userId: User.Id): F[Unit]

  def respondentDataSubmit(userId: User.Id): F[Unit]

  def sessionCreate(sessionNumber: Session.Number): F[Unit]

  def sessionJoin(
    sessionNumber: Session.Number,
    respondentId: User.Id): F[Unit]
