package jp.ac.tachibana.food_survey.services.user

import jp.ac.tachibana.food_survey.domain.user.User

trait UserService[F[_]]:

  def create(
    name: String,
    role: User.Role): F[User]

  def getAllByRole(role: User.Role): F[List[User]]
