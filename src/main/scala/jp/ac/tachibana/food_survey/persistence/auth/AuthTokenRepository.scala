package jp.ac.tachibana.food_survey.persistence.auth

import java.time.Instant

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.util.crypto.Hash

trait AuthTokenRepository[F[_]]:

  // todo: create index
  def insert(
    userId: User.Id,
    tokenHash: Hash,
    createdAt: Instant): F[Unit]

  def get(tokenHash: Hash): F[Option[User.Id]]

  def deleteByToken(tokenHash: Hash): F[Unit]

  def deleteByUser(userId: User.Id): F[Unit]
