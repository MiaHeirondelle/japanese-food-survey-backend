package jp.ac.tachibana.food_survey.services.session.managers

import java.util.concurrent.Semaphore

import cats.data.OptionT
import cats.effect.{Ref, Sync}
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
import jp.ac.tachibana.food_survey.persistence.session.SessionRepository
import jp.ac.tachibana.food_survey.services.session.SessionService

class DefaultCurrentSessionStateManager[F[_]: Monad](
  sessionRepository: SessionRepository[F],
  awaitingUsersSessionManager: AwaitingUsersSessionManager[F],
  inProgressSessionManager: InProgressSessionManager[F])
    extends CurrentSessionStateManager[F]:

  override def getLatestSessionNumber: F[Option[Session.Number]] =
    getLatestRegisteredState
      .map(_.number)
      .orElseF(sessionRepository.getLatestSessionNumber)
      .value

  override def getCurrentSession: F[Option[Session]] =
    OptionT(inProgressSessionManager.getCurrentState)
      .map(_.session: Session)
      .orElseF(awaitingUsersSessionManager.getCurrentState.widen)
      .orElseF(sessionRepository.getActiveSession.widen)
      .value

  override def createNewSession(session: Session.AwaitingUsers): F[Unit] =
    inProgressSessionManager.unregisterSession >> sessionRepository.createNewSession(session) >> awaitingUsersSessionManager
      .registerSession(session)

  override def registerInProgressSession(session: Session.InProgress): F[Unit] =
    awaitingUsersSessionManager.unregisterSession >> inProgressSessionManager.registerSession(session)

  override def finishInProgressSession: F[Option[Session.Finished]] =
    inProgressSessionManager.getCurrentState
      .flatMap {
        case Some(element: SessionService.SessionElementState) =>
          element.session match {
            case finishedSession: Session.Finished =>
              finishSession(finishedSession).as(finishedSession.some)
            case _ =>
              none[Session.Finished].pure[F]
          }
        case _ =>
          none[Session.Finished].pure[F]
      }

  override def refreshAwaitingUsersSessionManager: F[Unit] =
    // If some state is registered, a refresh is not necessary
    getLatestRegisteredState
      .flatTapNone(
        sessionRepository.getActiveSession.flatTap(_.traverse(awaitingUsersSessionManager.registerSession)).void
      )
      .value
      .void

  private def getLatestRegisteredState: OptionT[F, Session] =
    OptionT(awaitingUsersSessionManager.getCurrentState)
      .widen[Session]
      .orElse(OptionT(inProgressSessionManager.getCurrentState).map(_.session))

  override def stop: F[Unit] =
    inProgressSessionManager.getCurrentState
      .flatMap {
        case Some(element: SessionService.SessionElementState) =>
          element.session match {
            case finishedSession: Session.Finished =>
              finishSession(finishedSession)
            case session: Session.InProgress =>
              finishSession(Session.Finished.fromInProgress(session))
          }
        case _ =>
          ().pure[F]
      }

  private def unregisterAll: F[Unit] =
    inProgressSessionManager.unregisterSession >> awaitingUsersSessionManager.unregisterSession

  private def finishSession(finishedSession: Session.Finished): F[Unit] =
    sessionRepository.finishSession(finishedSession) >> unregisterAll
