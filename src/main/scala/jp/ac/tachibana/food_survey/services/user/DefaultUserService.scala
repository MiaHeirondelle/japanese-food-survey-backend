package jp.ac.tachibana.food_survey.services.user
import cats.Monad
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.domain.user.User.Id
import jp.ac.tachibana.food_survey.persistence.user.UserRepository

class DefaultUserService[F[_]: Monad](userRepository: UserRepository[F]) extends UserService[F]:

  override def create(
    name: String,
    role: User.Role): F[User] =
    generateUser(name, role).flatTap(userRepository.insert)

  private def generateUser(
    name: String,
    role: User.Role): F[User] =
    generateUserId.map(User(_, name, role))

  // todo: proper id generation
  private def generateUserId: F[User.Id] =
    User.Id("test").pure[F]
