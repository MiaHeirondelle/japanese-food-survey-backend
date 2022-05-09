package jp.ac.tachibana.food_survey.persistence.user

import jp.ac.tachibana.food_survey.domain.user.{User, UserData}

trait UserDataRepository[F[_]]:

  def insert(userData: UserData): F[Unit]

  def get(userId: User.Id): F[Option[UserData]]
