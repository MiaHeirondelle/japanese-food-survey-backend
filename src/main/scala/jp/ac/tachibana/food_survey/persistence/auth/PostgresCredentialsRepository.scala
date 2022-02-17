package jp.ac.tachibana.food_survey.persistence.auth

import cats.effect.Async
import cats.syntax.applicative.*
import cats.syntax.functor.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*

import jp.ac.tachibana.food_survey.domain.user.{User, UserCredentials}
import jp.ac.tachibana.food_survey.persistence.util.ParameterInstances.*
import jp.ac.tachibana.food_survey.services.auth.domain.HashedUserCredentials
import jp.ac.tachibana.food_survey.util.crypto.{Hash, Salt, Secret}

class PostgresCredentialsRepository[F[_]: Async](implicit tr: Transactor[F]) extends CredentialsRepository[F]:

  override def insert(credentials: CredentialsRepository.StoredCredentials): F[Unit] =
    import credentials.*
    sql"""INSERT INTO "user_credentials" (user_id, login, password_hash, password_salt)
         |VALUES ($userId, $login, $passwordHash, $passwordSalt)""".stripMargin.update.run
      .transact(tr)
      .void

  override def getByLogin(login: UserCredentials.Login): F[Option[CredentialsRepository.StoredCredentials]] =
    sql"""SELECT user_id, login, password_hash, password_salt FROM "user_credentials"
         |WHERE user_credentials.login = $login
         |""".stripMargin
      .query[CredentialsRepository.StoredCredentials]
      .option
      .transact(tr)
