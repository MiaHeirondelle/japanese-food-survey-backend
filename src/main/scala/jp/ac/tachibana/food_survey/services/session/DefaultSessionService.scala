package jp.ac.tachibana.food_survey.services.session

import cats.Monad
import cats.data.{NonEmptyList, OptionT}
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.persistence.session.SessionRepository
import jp.ac.tachibana.food_survey.persistence.user.UserRepository

class DefaultSessionService[F[_]: Monad](
  sessionRepository: SessionRepository[F],
  userRepository: UserRepository[F])
    extends SessionService[F]:

  // todo: current session state (cached)

  override def getActiveSession: F[Option[Session]] =
    sessionRepository.getActiveSession

  override def create(
    creator: User.Admin,
    respondents: NonEmptyList[User.Id]): F[Either[SessionService.SessionCreationError, Session.AwaitingUsers]] =
    for {
      sessionOpt <- getActiveSession
      result <- sessionOpt match {
        case None =>
          for {
            // todo: optimize
            respondentsOpt <- respondents
              .traverse(id => OptionT(userRepository.get(id)).collect { case user: User.Respondent => user })
              .value
            result <- respondentsOpt match {
              case Some(respondents) =>
                for {
                  sessionNumberOpt <- sessionRepository.getLatestSessionNumber
                  sessionNumber = sessionNumberOpt.fold(Session.Number.zero)(_.increment)
                  session = Session.AwaitingUsers(
                    number = sessionNumber,
                    joinedUsers = Nil,
                    waitingForUsers = respondents,
                    admin = creator
                  )
                  _ <- sessionRepository.createNewSession(session)
                } yield session.asRight[SessionService.SessionCreationError]
              case None =>
                SessionService.SessionCreationError.InvalidParticipants
                  .asLeft[Session.AwaitingUsers]
                  .pure[F]
            }
          } yield result

        case Some(_) =>
          SessionService.SessionCreationError.WrongSessionStatus
            .asLeft[Session.AwaitingUsers]
            .pure[F]
      }
    } yield result

  override def join(respondent: User.Respondent): F[Either[SessionService.SessionJoinError, Unit]] =
    // todo: check if user in awaiting session
    OptionT(sessionRepository.getActiveSession)
      .semiflatMap {
        case s: Session.AwaitingUsers =>
          // todo: not persist
          NonEmptyList.fromList(s.waitingForUsers.filterNot(_.id === respondent.id)) match {
            case Some(waitingForUsers) =>
              sessionRepository
                .updateSession(
                  s.copy(
                    joinedUsers = respondent :: s.joinedUsers,
                    waitingForUsers = waitingForUsers
                  ))
                .map(_.asRight[SessionService.SessionJoinError])

            case None =>
              // todo: not persist
              sessionRepository
                .updateSession(
                  Session.CanBegin(
                    number = s.number,
                    joinedUsers = NonEmptyList.of(respondent, s.joinedUsers*),
                    admin = s.admin
                  )
                )
                .map(_.asRight[SessionService.SessionJoinError])
          }

        case _ =>
          SessionService.SessionJoinError.WrongSessionStatus.asLeft[Unit].pure[F]
      }
      .getOrElse(SessionService.SessionJoinError.WrongSessionStatus.asLeft[Unit])

  override def begin(admin: User.Admin): F[Either[SessionService.SessionBeginError, Session.InProgress]] =
    // todo: check admin the same as creator?
    OptionT(sessionRepository.getActiveSession)
      .semiflatMap {
        case s: Session.CanBegin =>
          val session = Session.InProgress(
            number = s.number,
            joinedUsers = s.joinedUsers,
            admin = s.admin
          )
          sessionRepository
            .updateSession(
              session
            )
            .as(session.asRight[SessionService.SessionBeginError])

        case _ =>
          SessionService.SessionBeginError.WrongSessionStatus.asLeft[Session.InProgress].pure[F]
      }
      .getOrElse(SessionService.SessionBeginError.WrongSessionStatus.asLeft[Session.InProgress])

  override def update: F[Either[SessionService.SessionUpdateError, Session.InProgress]] = ???

  override def finish: F[Either[SessionService.SessionFinishError, Session.Finished]] = ???

  override def stop: F[Unit] =
    sessionRepository.reset
