package jp.ac.tachibana.food_survey.services.session.managers

import cats.Functor
import cats.data.{NonEmptyList, OptionT}
import cats.effect.Ref
import cats.syntax.either.*
import cats.syntax.eq.*
import cats.syntax.functor.*
import cats.syntax.option.*

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.services.session.SessionService

class DefaultAwaitingUsersSessionManager[F[_]: Functor](ref: Ref[F, Option[Session.NotBegan]])
    extends AwaitingUsersSessionManager[F]:

  override def registerSession(session: Session.AwaitingUsers): F[Unit] =
    ref.set(session.some)

  override def getCurrentState: F[Option[Session.NotBegan]] =
    ref.get

  // todo: check if user in awaiting session
  override def join(respondent: User.Respondent): F[Either[AwaitingUsersSessionManager.Error, Session.NotBegan]] =
    modifyNonEmpty {
      case s: Session.AwaitingUsers =>
        val newSession: Session.NotBegan =
          NonEmptyList.fromList(s.waitingForUsers.filterNot(_.id === respondent.id)) match {
            case Some(waitingForUsers) =>
              s.copy(
                joinedUsers = respondent :: s.joinedUsers,
                waitingForUsers = waitingForUsers
              )

            case None =>
              Session.CanBegin(
                number = s.number,
                joinedUsers = NonEmptyList.of(respondent, s.joinedUsers*),
                admin = s.admin
              )
          }

        (newSession.some, newSession.asRight)

      case s: Session.CanBegin =>
        (s.some, AwaitingUsersSessionManager.Error.InvalidSessionState.asLeft)
    }

  override def unregisterSession: F[Unit] =
    ref.set(none)

  private def modifyNonEmpty[A](
    f: Session.NotBegan => DefaultAwaitingUsersSessionManager.StateTransformationResult[A])
    : F[DefaultAwaitingUsersSessionManager.OperationResult[A]] =
    ref.modify(stateOpt => stateOpt.fold((stateOpt, AwaitingUsersSessionManager.Error.InvalidSessionState.asLeft[A]))(f))

object DefaultAwaitingUsersSessionManager:

  private type OperationResult[A] =
    Either[AwaitingUsersSessionManager.Error, A]

  private type StateTransformationResult[A] =
    (Option[Session.NotBegan], OperationResult[A])
