package jp.ac.tachibana.food_survey.persistence.user

import cats.effect.Async
import cats.syntax.applicative.*
import cats.syntax.functor.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import jp.ac.tachibana.food_survey.domain.auth.HashedUserCredentials
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.persistence.formats.ParameterInstances.*

class PostgresUserRepository[F[_]: Async](implicit tr: Transactor[F]) extends UserRepository[F]:

  override def insert(user: User): F[Unit] =
    import user.*
    sql"""INSERT INTO "user" (id, name, role) VALUES ($id, $name, $role)""".update.run
      .transact(tr)
      .void

  override def get(userId: User.Id): F[Option[User]] =
    sql"""SELECT id, name, role FROM "user"
         |WHERE id = $userId""".stripMargin
      .query[User]
      .option
      .transact(tr)

  override def getByCredentials(credentials: HashedUserCredentials): F[Option[User]] =
    import credentials.*
    sql"""SELECT "user".id, "user".name, "user".role FROM "user_credentials"
         |JOIN "user" ON user_credentials.user_id = "user".id
         |WHERE "user_credentials".login = $login
         |AND "user_credentials".password_hash = $passwordHash
         |AND "user_credentials".password_salt = $passwordSalt""".stripMargin
      .query[User]
      .option
      .transact(tr)

  override def getAllByRole(role: User.Role): F[List[User]] =
    sql"""SELECT id, name, $role FROM "user"
         |WHERE role = $role""".stripMargin
      .query[User]
      .to[List]
      .transact(tr)
