package jp.ac.tachibana.food_survey.persistence.user

import cats.effect.Async
import doobie.Transactor
import jp.ac.tachibana.food_survey.domain.user.User.Id
import jp.ac.tachibana.food_survey.domain.user.{User, UserData}
import doobie.implicits.*
import cats.syntax.functor.*
import jp.ac.tachibana.food_survey.domain.auth.HashedUserCredentials
import jp.ac.tachibana.food_survey.persistence.formats.UserInstances.*

class PostgresUserDataRepository[F[_]: Async](implicit tr: Transactor[F]) extends UserDataRepository[F]:

  override def insert(userData: UserData): F[Unit] =
    import userData.*
    sql"""INSERT INTO "user_data" (user_id, sex, age) VALUES ($userId, $sex, $age)""".update.run
      .transact(tr)
      .void

  override def get(userId: User.Id): F[Option[UserData]] =
    sql"""SELECT user_id, sex, age FROM "user_data"
         |WHERE user_id = $userId""".stripMargin
      .query[UserData]
      .option
      .transact(tr)
