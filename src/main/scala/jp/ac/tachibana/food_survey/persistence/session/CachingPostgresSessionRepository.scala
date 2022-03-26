package jp.ac.tachibana.food_survey.persistence.session

import java.util.concurrent.Semaphore

import cats.effect.{Async, Concurrent, Ref}
import cats.syntax.applicative.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.order.*
import cats.syntax.traverse.*
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
    underlying.createNewSession(session) >> setCache(session)

  override def setActiveSession(session: Session): F[Unit] =
    session match {
      case s: Session.AwaitingUsers =>
        underlying.setActiveSession(s) >> setCacheToLatest(s)
      case s: Session.CanBegin =>
        setCacheToLatest(s)
      case s: Session.InProgress =>
        setCacheToLatest(s)
      case s: Session.Finished =>
        for {
          _ <- underlying.setActiveSession(s)
          cachedSessionOpt <- activeSessionCache.get
          result <- resetCache.whenA(cachedSessionOpt.exists(_.number === s.number))
        } yield result
    }

  override def updateInProgressSession(
    update: Session.InProgress => Session.InProgressOrFinished): F[Option[Session.InProgressOrFinished]] =
    for {
      _ <- getActiveSession // toggle cache refresh if it's empty
      session <- updateCache(update)
      _ <- session match {
        case Some(s: Session.Finished) =>
          underlying.setActiveSession(s)
        case Some(s: Session.InProgress) =>
          Applicative[F].unit
        case None =>
          Applicative[F].unit
      }
    } yield session

  override def reset: F[Unit] =
    underlying.reset >> resetCache

  private def setCacheToLatest(session: Session.NotFinished): F[Unit] =
    for {
      cachedSession <- activeSessionCache.get
      result <- if (cachedSession.exists(_.number <= session.number)) setCache(session) else Applicative[F].unit
    } yield result

  private def setCache(session: Session.NotFinished): F[Unit] =
    activeSessionCache.set(Some(session)).void

  private def updateCache(update: Session.InProgress => Session.InProgressOrFinished): F[Option[Session.InProgressOrFinished]] =
    activeSessionCache.modify(_.fold[(Option[Session.NotFinished], Option[Session.InProgressOrFinished])]((None, None)) {
      case s: Session.AwaitingUsers => (Some(s), None)
      case s: Session.CanBegin      => (Some(s), None)
      case s: Session.InProgress =>
        update(s) match {
          case us: Session.InProgress =>
            (Some(us), Some(us))
          case us: Session.Finished =>
            (None, Some(us))
        }
    })

  private def resetCache: F[Unit] =
    activeSessionCache.set(None)

  private def refreshCache: F[Option[Session.NotFinished]] =
    underlying.getActiveSession.flatTap(activeSessionCache.getAndSet)

object CachingPostgresSessionRepository:

  def make[F[_]: Concurrent: Async: Transactor]: F[SessionRepository[F]] =
    val underlying = new PostgresSessionRepository[F]()
    Ref.of[F, Option[Session.NotFinished]](None).map(new CachingPostgresSessionRepository[F](underlying, _))
