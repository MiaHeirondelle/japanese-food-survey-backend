package jp.ac.tachibana.food_survey.persistence.auth

import java.time.Instant

import cats.effect.Async
import cats.syntax.functor.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.persistence.formats.ParameterInstances.*
import jp.ac.tachibana.food_survey.util.crypto.Hash

class PostgresAuthTokenRepository[F[_]: Async](implicit tr: Transactor[F]) extends AuthTokenRepository[F]:

  override def insert(
    userId: User.Id,
    tokenHash: Hash,
    createdAt: Instant): F[Unit] =
    sql"""INSERT INTO "user_session" (user_id, token_hash, created_at) VALUES ($userId, $tokenHash, $createdAt)""".update.run
      .transact(tr)
      .void

  override def get(tokenHash: Hash): F[Option[User.Id]] =
    sql"""SELECT user_id FROM "user_session" WHERE token_hash = $tokenHash"""
      .query[User.Id]
      .option
      .transact(tr)

  override def delete(tokenHash: Hash): F[Unit] =
    sql"""DELETE FROM "user_session" WHERE token_hash = $tokenHash""".update.run
      .transact(tr)
      .void
