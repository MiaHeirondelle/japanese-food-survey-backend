package jp.ac.tachibana.food_survey.services.user

import jp.ac.tachibana.food_survey.domain.user.User

trait UserService[F[_]] {

  def create(user: User): F[Unit]

  def get(userId: User.Id): F[Option[User]]
}
