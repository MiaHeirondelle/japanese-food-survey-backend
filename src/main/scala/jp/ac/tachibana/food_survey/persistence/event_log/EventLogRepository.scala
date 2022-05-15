package jp.ac.tachibana.food_survey.persistence.event_log

import jp.ac.tachibana.food_survey.domain.event_log.EventLog

trait EventLogRepository[F[_]]:

  def insert(eventLog: EventLog): F[Unit]
