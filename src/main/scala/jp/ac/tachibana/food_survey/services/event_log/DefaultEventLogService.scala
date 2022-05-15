package jp.ac.tachibana.food_survey.services.event_log

import cats.MonadThrow
import cats.effect.Clock
import cats.syntax.applicativeError.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import jp.ac.tachibana.food_survey.domain.event_log.EventLog
import jp.ac.tachibana.food_survey.domain.question.{Question, QuestionAnswer}
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.persistence.event_log.EventLogRepository

import java.time.Instant

class DefaultEventLogService[F[_]: MonadThrow: Clock](repository: EventLogRepository[F]) extends EventLogService[F]:

  override def userLogin(userId: User.Id): F[Unit] =
    constructEventLog(EventLog.EventType.UserLogin, userId = userId.some).flatMap(persistEventLog)

  override def respondentDataSubmit(userId: User.Id): F[Unit] =
    constructEventLog(EventLog.EventType.RespondentDataSubmit, userId = userId.some).flatMap(persistEventLog)

  override def sessionCreate(sessionNumber: Session.Number): F[Unit] =
    constructEventLog(EventLog.EventType.SessionCreate, sessionNumber = sessionNumber.some).flatMap(persistEventLog)

  override def sessionJoin(
    sessionNumber: Session.Number,
    respondentId: User.Id): F[Unit] =
    constructEventLog(EventLog.EventType.SessionJoin, sessionNumber = sessionNumber.some, userId = respondentId.some)
      .flatMap(persistEventLog)

  override def sessionBegin(sessionNumber: Session.Number): F[Unit] =
    constructEventLog(EventLog.EventType.SessionBegin, sessionNumber = sessionNumber.some).flatMap(persistEventLog)

  override def answerSubmit(answer: QuestionAnswer): F[Unit] =
    constructEventLog(
      EventLog.EventType.AnswerSubmit,
      sessionNumber = answer.sessionNumber.some,
      userId = answer.respondentId.some,
      questionId = answer.questionId.some).flatMap(persistEventLog)

  override def sessionPause(sessionNumber: Session.Number): F[Unit] =
    constructEventLog(EventLog.EventType.SessionPause, sessionNumber = sessionNumber.some).flatMap(persistEventLog)

  override def sessionResume(sessionNumber: Session.Number): F[Unit] =
    constructEventLog(EventLog.EventType.SessionResume, sessionNumber = sessionNumber.some).flatMap(persistEventLog)

  override def sessionFinish(sessionNumber: Session.Number): F[Unit] =
    constructEventLog(EventLog.EventType.SessionFinish, sessionNumber = sessionNumber.some).flatMap(persistEventLog)

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
