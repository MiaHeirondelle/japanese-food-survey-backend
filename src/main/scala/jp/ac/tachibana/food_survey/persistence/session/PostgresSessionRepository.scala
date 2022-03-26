package jp.ac.tachibana.food_survey.persistence.session

import cats.data.NonEmptyList
import cats.effect.{Async, Ref, Sync}
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.traverse.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.circe.jsonb.implicits.*
import doobie.postgres.implicits.*

import jp.ac.tachibana.food_survey.domain.question.QuestionAnswer
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.persistence.util.ParameterInstances.*
import jp.ac.tachibana.food_survey.persistence.util.SessionInstances.SessionPostgresFormat
import jp.ac.tachibana.food_survey.services.auth.domain.HashedUserCredentials

class PostgresSessionRepository[F[_]: Async](implicit tr: Transactor[F]) extends SessionRepository[F]:

  // todo: trigger on status - only one active session
  override def getLatestSessionNumber: F[Option[Session.Number]] =
    sql"""SELECT session_number FROM survey_session ORDER BY session_number DESC LIMIT 1"""
      .query[Session.Number]
      .option
      .transact(tr)

  override def getActiveSession: F[Option[Session.NotFinished]] =
    val query = selectActiveSessionQuery
      .flatMap(_.traverse(activeSession =>
        for {
          respondents <- selectSessionRespondentsQuery(activeSession.number)
          admin <- selectSessionAdminQuery(activeSession.admin)
          result: Session.NotFinished <- activeSession match {
            case s: SessionPostgresFormat.AwaitingUsers =>
              Session
                .AwaitingUsers(
                  number = s.number,
                  joinedUsers = Nil,
                  waitingForUsers = respondents,
                  admin = admin
                )
                .pure[ConnectionIO]
          }
        } yield result))
    query.transact(tr)

  private val selectActiveSessionQuery: ConnectionIO[Option[SessionPostgresFormat.AwaitingUsers]] =
    sql"""SELECT session_number, admin_id, status, state FROM "survey_session" WHERE status != 'finished'"""
      .query[SessionPostgresFormat.AwaitingUsers]
      .option

  private def selectSessionRespondentsQuery(sessionNumber: Session.Number): ConnectionIO[NonEmptyList[User.Respondent]] =
    sql"""SELECT id, name FROM "user"
         |JOIN "survey_session_participant" AS ssp ON ssp.user_id = "user".id AND ssp.session_number = $sessionNumber""".stripMargin
      .query[User.Respondent]
      .nel

  private def selectSessionAdminQuery(userId: User.Id): ConnectionIO[User.Admin] =
    sql"""SELECT id, name FROM "user"
         |WHERE id = $userId AND role = 'admin'""".stripMargin
      .query[User.Admin]
      .unique

  override def createNewSession(session: Session.AwaitingUsers): F[Unit] =
    val query = for {
      _ <- insertNewSessionQuery(session)
      _ <- insertParticipantsQuery(session)
    } yield ()
    query.transact(tr)

  private def insertNewSessionQuery(session: Session.AwaitingUsers) =
    val data = SessionPostgresFormat.fromDomain(session)
    sql"""INSERT INTO "survey_session" (session_number, admin_id, status, state) VALUES ($data)""".update.run

  private def insertParticipantsQuery(session: Session.AwaitingUsers) =
    val data = session.waitingForUsers.map(u => (session.number, u.id))
    Update[(Session.Number, User.Id)]("""INSERT INTO "survey_session_participant" (session_number, user_id) VALUES (?, ?)""")
      .updateMany(data)

  override def setActiveSession(session: Session): F[Unit] =
    val encoded = SessionPostgresFormat.fromDomain(session)
    val encodedState = encoded.asStateJson
    sql"""UPDATE "survey_session" SET
          |admin_id = ${encoded.admin},
          |status = ${encoded.encodedStatus},
          |state = $encodedState
          |WHERE session_number = ${encoded.number}
        """.stripMargin.update.run
      .transact(tr)
      .void

  // todo: comment no-op
  override def updateInProgressSession(
    update: Session.InProgress => Session.InProgressOrFinished): F[Option[Session.InProgressOrFinished]] =
    none[Session.InProgressOrFinished].pure[F]

  override def reset: F[Unit] =
    val query = for {
      _ <- sql"""DELETE FROM "survey_session_participant"""".update.run
      _ <- sql"""DELETE FROM "survey_session"""".update.run
    } yield ()
    query.transact(tr)
