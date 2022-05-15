package jp.ac.tachibana.food_survey.persistence.event_log

import cats.effect.Async
import doobie.Transactor
import jp.ac.tachibana.food_survey.domain.event_log.EventLog
import jp.ac.tachibana.food_survey.persistence.auth.CredentialsRepository
import jp.ac.tachibana.food_survey.persistence.formats.ParameterInstances.*
import doobie.postgres.implicits.*
import doobie.implicits.*
import cats.syntax.functor.*

class PostgresEventLogRepository[F[_]: Async](implicit tr: Transactor[F]) extends EventLogRepository[F]:

  override def insert(eventLog: EventLog): F[Unit] =
    import eventLog.*
    sql"""INSERT INTO "event_log" (event_type, session_number, user_id, question_id, timestamp_utc)
         |VALUES ($eventType, $sessionNumber, $userId, $questionId, $createdAt)""".stripMargin.update.run
      .transact(tr)
      .void
