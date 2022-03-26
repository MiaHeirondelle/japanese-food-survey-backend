package jp.ac.tachibana.food_survey.services.session

import cats.Monad
import cats.data.{EitherT, NonEmptyList, OptionT}
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*

import jp.ac.tachibana.food_survey.domain.question.QuestionAnswer
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.persistence.session.{SessionRepository, SessionTemplateRepository}
import jp.ac.tachibana.food_survey.persistence.user.UserRepository
import jp.ac.tachibana.food_survey.services.session.managers.InProgressSessionManager.Error
import jp.ac.tachibana.food_survey.services.session.managers.{AwaitingUsersSessionManager, InProgressSessionManager}

class DefaultSessionService[F[_]: Monad](
  sessionRepository: SessionRepository[F],
  sessionTemplateRepository: SessionTemplateRepository[F],
  userRepository: UserRepository[F],
  awaitingUsersSessionManager: AwaitingUsersSessionManager[F],
  inProgressSessionManager: InProgressSessionManager[F])
    extends SessionService[F]:

  // todo: current session state (cached)

  override def getActiveSession: F[Option[Session.NotFinished]] =
    OptionT(inProgressSessionManager.getCurrentState)
      .subflatMap {
        case s: SessionService.SessionElementState.Finished =>
          none

        case s: SessionService.SessionElementState.Question =>
          s.session.some
      }
      .orElseF(awaitingUsersSessionManager.getCurrentState.widen)
      .orElseF(sessionRepository.getActiveSession.widen)
      .value

  override def create(
    creator: User.Admin,
    respondents: NonEmptyList[User.Id]): F[Either[SessionService.CreateSessionError, Session.AwaitingUsers]] =
    for {
      // todo: get session structure and persist session snapshot
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
                  _ <- awaitingUsersSessionManager.registerSession(session)
                } yield session.asRight[SessionService.CreateSessionError]
              case None =>
                SessionService.CreateSessionError.InvalidParticipants
                  .asLeft[Session.AwaitingUsers]
                  .pure[F]
            }
          } yield result

        case Some(_) =>
          SessionService.CreateSessionError.WrongSessionStatus
            .asLeft[Session.AwaitingUsers]
            .pure[F]
      }
    } yield result

  override def join(respondent: User.Respondent): F[Either[SessionService.JoinSessionError, Session.NotBegan]] =
    awaitingUsersSessionManager
      .join(respondent)
      .map(_.leftMap {
        case AwaitingUsersSessionManager.Error.InvalidSessionState =>
          SessionService.JoinSessionError.WrongSessionStatus
        case AwaitingUsersSessionManager.Error.InvalidParticipant =>
          SessionService.JoinSessionError.InvalidParticipant
      })

  override def begin(admin: User.Admin): F[Either[SessionService.BeginSessionError, Session.InProgress]] =
    // todo: check admin the same as creator?
    OptionT(awaitingUsersSessionManager.getCurrentState)
      .semiflatMap {
        case s: Session.CanBegin =>
          for {
            template <- sessionTemplateRepository.getActiveTemplate
            newSession = Session.InProgress.fromTemplate(s, template)
            _ <- inProgressSessionManager.registerSession(newSession)
            _ <- awaitingUsersSessionManager.unregisterSession
          } yield newSession.asRight[SessionService.BeginSessionError]

        case _ =>
          SessionService.BeginSessionError.WrongSessionStatus.asLeft[Session.InProgress].pure[F]
      }
      .getOrElse(SessionService.BeginSessionError.WrongSessionStatus.asLeft[Session.InProgress])

  override def provideAnswer(
    answer: QuestionAnswer): F[Either[SessionService.ProvideAnswerError, SessionService.SessionElementState.Question]] =
    inProgressSessionManager
      .provideAnswer(answer)
      .map(_.leftMap { case InProgressSessionManager.Error.IncorrectSessionState =>
        SessionService.ProvideAnswerError.IncorrectSessionState
      })

  override def transitionToNextElement
    : F[Either[SessionService.TransitionToNextElementError, Option[SessionService.SessionElementState]]] =
    inProgressSessionManager.transitionToNextElement
      .map(_.leftMap { case InProgressSessionManager.Error.IncorrectSessionState =>
        SessionService.TransitionToNextElementError.WrongSessionStatus
      })

  override def finish: F[Either[SessionService.FinishSessionError, Session.Finished]] = ???

  override def stop: F[Unit] =
    inProgressSessionManager.unregisterSession >> awaitingUsersSessionManager.unregisterSession >> sessionRepository.reset
