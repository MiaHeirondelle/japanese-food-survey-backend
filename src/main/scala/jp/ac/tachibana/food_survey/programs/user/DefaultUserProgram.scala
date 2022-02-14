package jp.ac.tachibana.food_survey.programs.user

import cats.Monad
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.applicative.*

import jp.ac.tachibana.food_survey.domain.user.User.Role
import jp.ac.tachibana.food_survey.domain.user.{User, UserCredentials}
import jp.ac.tachibana.food_survey.services.user.UserService
import jp.ac.tachibana.food_survey.services.auth.AuthenticationService

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

  private def generateUser(
    name: String,
    role: User.Role): F[User] =
    generateUserId.map(userId =>
      role match {
        case User.Role.Respondent =>
          User.Respondent(userId, name)
        case User.Role.Admin =>
          User.Admin(userId, name)
      })

  // todo: proper id generation
  private def generateUserId: F[User.Id] =
    User.Id("test").pure[F]
