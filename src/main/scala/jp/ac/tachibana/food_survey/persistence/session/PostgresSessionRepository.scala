package jp.ac.tachibana.food_survey.persistence.session

import cats.effect.{Async, Ref, Sync}

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import cats.syntax.functor.*
import doobie.Transactor

class PostgresSessionRepository[F[_]: Async](implicit tr: Transactor[F]) extends SessionRepository[F]:

  override def getLatestSessionNumber: F[Option[Session.Number]] = ???

  override def getActiveSession: F[Option[Session]] =
    ???

  override def createNewSession(session: Session.AwaitingUsers): F[Unit] =
    ???

  override def updateActiveSession(session: Session): F[Unit] =
    ???

  override def reset: F[Unit] =
    ???
