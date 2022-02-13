package jp.ac.tachibana.food_survey.persistence.auth

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.services.auth.domain.HashedUserCredentials

trait CredentialsRepository[F[_]]:

  def insert(credentials: HashedUserCredentials): F[Unit]

  def get(userId: User.Id): F[Option[HashedUserCredentials]]
