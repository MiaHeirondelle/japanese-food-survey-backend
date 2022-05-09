package jp.ac.tachibana.food_survey.persistence.user

import cats.effect.Async
import doobie.Transactor
import jp.ac.tachibana.food_survey.domain.user.User.Id
import jp.ac.tachibana.food_survey.domain.user.{RespondentData, User}
import doobie.implicits.*
import cats.syntax.functor.*
import jp.ac.tachibana.food_survey.domain.auth.HashedUserCredentials
import jp.ac.tachibana.food_survey.persistence.formats.UserInstances.*

class PostgresRespondentDataRepository[F[_]: Async](implicit tr: Transactor[F]) extends RespondentDataRepository[F]:

  override def insert(respondentData: RespondentData): F[Unit] =
    import respondentData.*
    sql"""INSERT INTO "respondent_data" (user_id, sex, age) VALUES ($userId, $sex, $age)""".update.run
      .transact(tr)
      .void

  override def checkRespondentDataExists(userId: User.Id): F[Boolean] =
    sql"""SELECT COUNT(*) > 0 FROM "respondent_data"
         |WHERE user_id = $userId""".stripMargin
      .query[Boolean]
      .unique
      .transact(tr)
