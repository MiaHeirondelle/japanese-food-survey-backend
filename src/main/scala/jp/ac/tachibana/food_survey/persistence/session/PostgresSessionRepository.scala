package jp.ac.tachibana.food_survey.persistence.session

import cats.effect.{Async, Ref, Sync}
import cats.syntax.applicative.*
import cats.syntax.functor.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.circe.jsonb.implicits.*
import doobie.postgres.implicits.*

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.persistence.util.ParameterInstances.*
import jp.ac.tachibana.food_survey.persistence.util.SessionInstances.SessionPostgresFormat
import jp.ac.tachibana.food_survey.persistence.util.UserInstances.*
import jp.ac.tachibana.food_survey.services.auth.domain.HashedUserCredentials

class PostgresSessionRepository[F[_]: Async](implicit tr: Transactor[F]) extends SessionRepository[F]:

  // todo: trigger on status - only one active session
  override def getLatestSessionNumber: F[Option[Session.Number]] =
    sql"""SELECT session_number FROM survey_session ORDER BY session_number DESC LIMIT 1"""
      .query[Session.Number]
      .option
      .transact(tr)

  // todo: select participants
  override def getActiveSession: F[Option[Session]] =
    sql"""SELECT session_number, admin_id, status, state FROM "survey_session" WHERE status != 'finished'"""
      .query[SessionPostgresFormat]
      .map {
        case s: SessionPostgresFormat.AwaitingUsers =>
          Session.AwaitingUsers(
            number = s.number,
            joinedUsers = s.joinedUsers.map(id => User.Respondent(id, "test" + id.value)),
            waitingForUsers = s.waitingForUsers.map(id => User.Respondent(id, "test" + id.value)),
            admin = User.Admin(s.admin, "test" + s.admin.value)
          )
        case s: SessionPostgresFormat.CanBegin =>
          Session.CanBegin(
            number = s.number,
            joinedUsers = s.joinedUsers.map(id => User.Respondent(id, "test" + id.value)),
            admin = User.Admin(s.admin, "test" + s.admin.value)
          )
        case s: SessionPostgresFormat.InProgress =>
          Session.InProgress(
            number = s.number,
            joinedUsers = s.joinedUsers.map(id => User.Respondent(id, "test" + id.value)),
            admin = User.Admin(s.admin, "test" + s.admin.value)
          )
        case s: SessionPostgresFormat.Finished =>
          Session.Finished(
            number = s.number,
            joinedUsers = s.joinedUsers.map(id => User.Respondent(id, "test" + id.value)),
            admin = User.Admin(s.admin, "test" + s.admin.value)
          )
      }
      .option
      .transact(tr)

  // todo: insert participants
  override def createNewSession(session: Session.AwaitingUsers): F[Unit] = {
    val encoded = SessionPostgresFormat.fromDomain(session)
    sql"""INSERT INTO "survey_session" (session_number, admin_id, status, state) VALUES ($encoded)""".update.run
      .transact(tr)
      .void
  }

  override def updateSession(session: Session): F[Unit] =
    val encoded = SessionPostgresFormat.fromDomain(session)
    val encodedState = encoded.asStateJson
    sql"""UPDATE "survey_session" SET
          |admin_id = ${encoded.admin},
          |status = ${encoded.status},
          |state = $encodedState
          |WHERE session_number = ${encoded.number}
        """.stripMargin.update.run
      .transact(tr)
      .void

  override def reset: F[Unit] =
    sql"""DELETE FROM "survey_session"""".update.run.transact(tr).void
