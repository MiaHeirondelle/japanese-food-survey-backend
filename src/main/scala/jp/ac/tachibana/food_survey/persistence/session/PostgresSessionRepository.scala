package jp.ac.tachibana.food_survey.persistence.session

import cats.effect.{Ref, Sync}

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import cats.syntax.functor.*

class PostgresSessionRepository[F[_]: Sync](ref: Ref[F, Option[Session]]) extends SessionRepository[F]:

  override def getActiveSession: F[Option[Session]] =
    ref.get

  override def createNewSession(session: Session.AwaitingUsers): F[Unit] =
    ref.getAndSet(Some(session)).void

  override def updateActiveSession(session: Session): F[Unit] =
    ref.getAndSet(Some(session)).void

  override def reset: F[Unit] =
    ref.set(None)
