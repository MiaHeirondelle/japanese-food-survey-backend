package jp.ac.tachibana.food_survey.persistence.session

import java.util.concurrent.Semaphore

import cats.effect.{Async, Concurrent, Ref}
import cats.syntax.applicative.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.order.*
import cats.{Applicative, Monad}
import doobie.Transactor

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.session.Session.Status

// todo: purpose
// todo: lock
class CachingPostgresSessionRepository[F[_]: Monad](
  underlying: PostgresSessionRepository[F],
  activeSessionCache: Ref[F, Option[Session.NotFinished]])
    extends SessionRepository[F]:

  override def getLatestSessionNumber: F[Option[Session.Number]] =
    underlying.getLatestSessionNumber

  override def getActiveSession: F[Option[Session.NotFinished]] =
    for {
      cachedSession <- activeSessionCache.get
      session <- cachedSession.fold(refreshCache)(Some(_).pure[F])
    } yield session

  override def createNewSession(session: Session.AwaitingUsers): F[Unit] =
    underlying.createNewSession(session) >> updateCache(session)

  override def updateSession(session: Session): F[Unit] =
    session match {
      case s: Session.AwaitingUsers =>
        underlying.updateSession(s) >> updateCacheToLatest(s)
      case s: Session.CanBegin =>
        updateCacheToLatest(s)
      case s: Session.InProgress =>
        updateCacheToLatest(s)
      case s: Session.Finished =>
        for {
          _ <- underlying.updateSession(s)
          cachedSessionOpt <- activeSessionCache.get
          result <- resetCache.whenA(cachedSessionOpt.exists(_.number === s.number))
        } yield result
    }

  override def reset: F[Unit] =
    underlying.reset >> resetCache

  private def updateCacheToLatest(session: Session.NotFinished): F[Unit] =
    for {
      cachedSession <- activeSessionCache.get
      result <- if (cachedSession.exists(_.number <= session.number)) updateCache(session) else Applicative[F].unit
    } yield result

  private def updateCache(session: Session.NotFinished): F[Unit] =
    activeSessionCache.set(Some(session)).void

  private def resetCache: F[Unit] =
    activeSessionCache.set(None)

  private def refreshCache: F[Option[Session.NotFinished]] =
    underlying.getActiveSession.flatTap(activeSessionCache.getAndSet)

object CachingPostgresSessionRepository:

  def make[F[_]: Concurrent: Async: Transactor]: F[SessionRepository[F]] =
    val underlying = new PostgresSessionRepository[F]()
    Ref.of[F, Option[Session.NotFinished]](None).map(new CachingPostgresSessionRepository[F](underlying, _))
