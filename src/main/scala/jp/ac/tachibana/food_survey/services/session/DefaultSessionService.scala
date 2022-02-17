package jp.ac.tachibana.food_survey.services.session

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.persistence.session.SessionRepository
import jp.ac.tachibana.food_survey.persistence.user.UserRepository
import cats.Monad
import cats.data.NonEmptyList
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.data.OptionT

import jp.ac.tachibana.food_survey.domain.user.User

class DefaultSessionService[F[_]: Monad](
  sessionRepository: SessionRepository[F],
  userRepository: UserRepository[F])
    extends SessionService[F]:

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
                val session = Session.AwaitingUsers(
                  joinedUsers = Nil,
                  waitingForUsers = respondents,
                  admin = creator
                )
                sessionRepository.createNewSession(session).as(session.asRight[SessionService.SessionCreationError])
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

  override def start: F[Either[SessionService.SessionStartError, Session.InProgress]] = ???

  override def update: F[Either[SessionService.SessionUpdateError, Session.InProgress]] = ???

  override def finish: F[Either[SessionService.SessionFinishError, Session.Finished]] = ???
