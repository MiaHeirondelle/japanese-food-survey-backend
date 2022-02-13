package jp.ac.tachibana.food_survey.programs.user

import jp.ac.tachibana.food_survey.domain.user.{User, UserCredentials}
import jp.ac.tachibana.food_survey.services.user.UserService
import jp.ac.tachibana.food_survey.services.auth.AuthenticationService

class DefaultUserProgram[F[_]](
  authenticationService: AuthenticationService[F],
  userService: UserService[F])
    extends UserProgram[F]:

  override def create(
    name: String,
    role: User.Role,
    credentials: UserCredentials): F[User.Id] =
    ???

  override def get(userId: User.Id): F[Option[User]] =
    userService.get(userId)
