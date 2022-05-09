package jp.ac.tachibana.food_survey.services.user

import jp.ac.tachibana.food_survey.domain.user.{User, UserData}

trait UserService[F[_]]:

  def create(
    name: String,
    role: User.Role): F[User]

  def getAllByRole(role: User.Role): F[List[User]]

  def saveUserData(userData: UserData): F[Unit]

  def getUserData(userId: User.Id): F[Option[UserData]]
