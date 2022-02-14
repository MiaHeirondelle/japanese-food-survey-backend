package jp.ac.tachibana.food_survey.persistence.user
import cats.Applicative
import cats.syntax.applicative.*

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.domain.user.User.Id
import jp.ac.tachibana.food_survey.domain.user.UserCredentials.Login
import jp.ac.tachibana.food_survey.services.auth.domain.HashedUserCredentials

class PostgresUserRepository[F[_]: Applicative] extends UserRepository[F]:

  override def insert(user: User): F[Unit] =
    Applicative[F].unit

  override def get(userId: Id): F[Option[User]] =
    Some(User.Admin(userId, "test_name")).pure[F]

  override def getByCredentials(credentials: HashedUserCredentials): F[Option[User]] =
    Some(User.Admin(User.Id("test"), "test_name")).pure[F]
