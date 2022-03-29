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

import jp.ac.tachibana.food_survey.domain.question.{Question, QuestionAnswer}
import jp.ac.tachibana.food_survey.domain.session.{Session, SessionAnswers}
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.persistence.formats.ParameterInstances.*
import jp.ac.tachibana.food_survey.persistence.formats.QuestionInstances.{AnswerId, AnswerPostgresFormat, QuestionPostgresFormat}
import jp.ac.tachibana.food_survey.persistence.formats.SessionInstances.SessionPostgresFormat
import jp.ac.tachibana.food_survey.services.auth.domain.HashedUserCredentials

// todo: remove state
class PostgresSessionRepository[F[_]: Async](implicit tr: Transactor[F]) extends SessionRepository[F]:

  // todo: trigger on status - only one active session
  override def getLatestSessionNumber: F[Option[Session.Number]] =
    sql"""SELECT session_number FROM survey_session ORDER BY session_number DESC LIMIT 1"""
      .query[Session.Number]
      .option
      .transact(tr)

  override def getActiveSession: F[Option[Session.AwaitingUsers]] =
    val query = selectActiveSessionQuery
      .flatMap(_.traverse(activeSession =>
        for {
          respondents <- selectSessionRespondentsQuery(activeSession.number)
          admin <- selectSessionAdminQuery(activeSession.admin)
          result <- activeSession match {
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

  override def finishSession(session: Session.Finished): F[Unit] =
    val encoded = SessionPostgresFormat.fromDomain(session)
    val encodedState = encoded.asStateJson
    val update =
      for {
        _ <- updateSessionQuery(session)
        answers = session.answers.toMap.values.flatMap(_.toSortedMap.values).toList
        _ <- NonEmptyList.fromList(answers).traverse(insertAnswersQuery)
      } yield ()

    update.transact(tr)

  private def updateSessionQuery(session: Session) =
    val format = SessionPostgresFormat.fromDomain(session)
    sql"""UPDATE "survey_session" SET
         |admin_id = ${format.admin},
         |status = ${format.encodedStatus},
         |state = ${format.asStateJson}
         |WHERE session_number = ${format.number}
        """.stripMargin.update.run

  private def insertAnswersQuery(answers: NonEmptyList[QuestionAnswer]) =
    for {
      data <- answers.traverse(answer =>
        AnswerId
          .generate[ConnectionIO]
          .map(AnswerPostgresFormat.fromDomain(_, answer) match {
            case AnswerPostgresFormat(id, answerType, sessionNumber, respondentId, questionId, previousQuestionId) =>
              (id, answerType, sessionNumber, respondentId, questionId, previousQuestionId)
          }))
      result <- Update[(AnswerId, AnswerPostgresFormat.Type, Session.Number, User.Id, Question.Id, Option[Question.Id])](
        """INSERT INTO "answer" (id, type, session_number, respondent_id, question_id, previous_question_id) VALUES (?, ?, ?, ?, ?, ?)""")
        .updateMany(data)
    } yield result

  override def reset: F[Unit] =
    val query = for {
      latestSessionNumber <-
        sql"""SELECT session_number FROM survey_session WHERE status != 'finished' ORDER BY session_number DESC LIMIT 1"""
          .query[Session.Number]
          .option
      _ <- latestSessionNumber.traverse { sn =>
        for {
          _ <- sql"""DELETE FROM "answer" WHERE session_number = $sn""".update.run
          _ <- sql"""DELETE FROM "survey_session_participant" WHERE session_number = $sn""".update.run
          _ <- sql"""DELETE FROM "survey_session" WHERE session_number = $sn""".update.run
        } yield ()
      }
    } yield ()
    query.transact(tr)
