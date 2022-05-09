package jp.ac.tachibana.food_survey.persistence.user

import jp.ac.tachibana.food_survey.domain.auth.HashedUserCredentials
import jp.ac.tachibana.food_survey.domain.user.User

trait UserRepository[F[_]] {

  def insert(user: User): F[Unit]

  def get(userId: User.Id): F[Option[User]]

  def getByCredentials(credentials: HashedUserCredentials): F[Option[User]]

  def getAllByRole(role: User.Role): F[List[User]]
}
