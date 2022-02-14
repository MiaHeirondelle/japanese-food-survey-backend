package jp.ac.tachibana.food_survey.persistence.user

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.services.auth.domain.HashedUserCredentials

trait UserRepository[F[_]] {

  def insert(user: User): F[Unit]

  def get(userId: User.Id): F[Option[User]]

  def getByCredentials(credentials: HashedUserCredentials): F[Option[User]]
}
