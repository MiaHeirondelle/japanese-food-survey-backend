package jp.ac.tachibana.food_survey.persistence.authentication

import java.time.Instant

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.util.crypto.Hash

trait AuthTokenRepository[F[_]]:

  def save(
    userId: User.Id,
    tokenHash: Hash,
    createdAt: Instant): F[Unit]

  def load(tokenHash: Hash): F[Option[User.Id]]

  def remove(tokenHash: Hash): F[Unit]
