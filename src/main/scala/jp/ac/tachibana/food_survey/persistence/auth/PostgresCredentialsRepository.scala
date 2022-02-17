package jp.ac.tachibana.food_survey.persistence.auth

import cats.Applicative
import cats.syntax.applicative.*

import jp.ac.tachibana.food_survey.domain.user.{User, UserCredentials}
import jp.ac.tachibana.food_survey.services.auth.domain.HashedUserCredentials
import jp.ac.tachibana.food_survey.util.crypto.{Hash, Salt, Secret}

class PostgresCredentialsRepository[F[_]: Applicative] extends CredentialsRepository[F]:

  override def insert(credentials: CredentialsRepository.StoredCredentials): F[Unit] =
    Applicative[F].unit

  override def getByLogin(login: UserCredentials.Login): F[Option[CredentialsRepository.StoredCredentials]] =
    Some(
      CredentialsRepository.StoredCredentials(
        userId = User.Id("test"),
        login = Secret(UserCredentials.Login("test")),
        passwordHash = Hash("test"),
        salt = Salt("test")
      )).pure[F]
