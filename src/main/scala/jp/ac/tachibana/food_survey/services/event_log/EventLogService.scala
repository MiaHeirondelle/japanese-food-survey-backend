package jp.ac.tachibana.food_survey.services.event_log

import jp.ac.tachibana.food_survey.domain.user.User

trait EventLogService[F[_]]:

  def userLogin(userId: User.Id): F[Unit]
