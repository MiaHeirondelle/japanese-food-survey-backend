package jp.ac.tachibana.food_survey.services.event_log

import cats.MonadThrow
import cats.effect.Clock
import cats.syntax.applicativeError.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import jp.ac.tachibana.food_survey.domain.event_log.EventLog
import jp.ac.tachibana.food_survey.domain.question.Question
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.persistence.event_log.EventLogRepository

import java.time.Instant

class DefaultEventLogService[F[_]: MonadThrow: Clock](repository: EventLogRepository[F]) extends EventLogService[F]:

  override def userLogin(userId: User.Id): F[Unit] =
    constructEventLog(EventLog.EventType.UserLogin, userId = userId.some).flatMap(persistEventLog)

  private def constructEventLog(
    eventType: EventLog.EventType,
    sessionNumber: Option[Session.Number] = none,
    userId: Option[User.Id] = none,
    questionId: Option[Question.Id] = none): F[EventLog] =
    Clock[F].realTime.map(time =>
      EventLog(
        eventType,
        sessionNumber,
        userId,
        questionId,
        createdAt = Instant.ofEpochMilli(time.toMillis)
      ))

  private def persistEventLog(eventLog: EventLog): F[Unit] =
    repository.insert(eventLog).attempt.void
