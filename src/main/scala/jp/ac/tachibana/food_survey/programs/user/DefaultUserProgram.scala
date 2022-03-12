package jp.ac.tachibana.food_survey.programs.user

import cats.Monad
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import jp.ac.tachibana.food_survey.domain.user.User.Role
import jp.ac.tachibana.food_survey.domain.user.{User, UserCredentials}
import jp.ac.tachibana.food_survey.services.auth.AuthenticationService
import jp.ac.tachibana.food_survey.services.user.UserService

class DefaultUserProgram[F[_]: Monad](
  authenticationService: AuthenticationService[F],
  userService: UserService[F])
    extends UserProgram[F]:

  override def create(
    name: String,
    role: User.Role,
    credentials: UserCredentials): F[User.Id] =
    for {
      user <- userService.create(name, role)
      result <- authenticationService.saveCredentials(user.id, credentials)
    } yield user.id

  override def getAllByRole(role: Role): F[List[User]] =
    userService.getAllByRole(role)

  private def generateUser(
    name: String,
    role: User.Role): F[User] =
    generateUserId.map(User(_, name, role))

  // todo: proper id generation
  private def generateUserId: F[User.Id] =
    User.Id("test").pure[F]
