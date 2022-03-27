package jp.ac.tachibana.food_survey.services.session

import cats.data.{EitherT, NonEmptyList, OptionT}
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.traverse.*
import cats.{Applicative, Monad}

import jp.ac.tachibana.food_survey.domain.question.QuestionAnswer
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.persistence.session.{SessionRepository, SessionTemplateRepository}
import jp.ac.tachibana.food_survey.persistence.user.UserRepository
import jp.ac.tachibana.food_survey.services.session.managers.*
import jp.ac.tachibana.food_survey.services.session.managers.InProgressSessionManager.Error

class DefaultSessionService[F[_]: Monad](
  sessionTemplateRepository: SessionTemplateRepository[F],
  userRepository: UserRepository[F],
  currentSessionStateManager: CurrentSessionStateManager[F],
  awaitingUsersSessionManager: AwaitingUsersSessionManager[F],
  inProgressSessionManager: InProgressSessionManager[F])
    extends SessionService[F]:

  override def getActiveSession: F[Option[Session.NotFinished]] =
    OptionT(currentSessionStateManager.getCurrentSession).subflatMap {
      case s: Session.Finished =>
        none
      case s: Session.NotFinished =>
        s.some
    }.value

  override def create(
    creator: User.Admin,
    respondents: NonEmptyList[User.Id]): F[Either[SessionService.CreateSessionError, Session.AwaitingUsers]] =
    for {
      // todo: persist session snapshot
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
                  sessionNumberOpt <- currentSessionStateManager.getLatestSessionNumber
                  sessionNumber = sessionNumberOpt.fold(Session.Number.zero)(_.increment)
                  session = Session.AwaitingUsers(
                    number = sessionNumber,
                    joinedUsers = Nil,
                    waitingForUsers = respondents,
                    admin = creator
                  )
                  _ <- currentSessionStateManager.createNewSession(session)
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
    // A refresh may be necessary if the session is re-created from persisted data.
    currentSessionStateManager.refreshAwaitingUsersSessionManager >>
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
            _ <- currentSessionStateManager.registerInProgressSession(newSession)
          } yield newSession.asRight[SessionService.BeginSessionError]

        case _ =>
          SessionService.BeginSessionError.WrongSessionStatus.asLeft[Session.InProgress].pure[F]
      }
      .getOrElse(SessionService.BeginSessionError.WrongSessionStatus.asLeft[Session.InProgress])

  override def getCurrentElementState: F[Either[SessionService.GetCurrentElementStateError, SessionService.SessionElementState]] =
    inProgressSessionManager.getCurrentState
      .map(_.toRight(SessionService.GetCurrentElementStateError.IncorrectSessionState))

  override def provideAnswer(
    answer: QuestionAnswer): F[Either[SessionService.ProvideAnswerError, SessionService.SessionElementState.Question]] =
    inProgressSessionManager
      .provideAnswer(answer)
      .map(_.leftMap { case InProgressSessionManager.Error.IncorrectSessionState =>
        SessionService.ProvideAnswerError.IncorrectSessionState
      })

  override def transitionToNextElement
    : F[Either[SessionService.TransitionToNextElementError, SessionService.NonPendingSessionElementState]] =
    inProgressSessionManager.transitionToNextElement
      .map(_.leftMap { case InProgressSessionManager.Error.IncorrectSessionState =>
        SessionService.TransitionToNextElementError.WrongSessionStatus
      })

  override def transitionToNextElement(
    respondentId: User.Id): F[Either[SessionService.TransitionToNextElementError, SessionService.SessionElementState]] =
    inProgressSessionManager
      .transitionToNextElement(respondentId)
      .map(_.leftMap { case InProgressSessionManager.Error.IncorrectSessionState =>
        SessionService.TransitionToNextElementError.WrongSessionStatus
      })

  override def finish: F[Either[SessionService.FinishSessionError, Session.Finished]] =
    currentSessionStateManager.finishInProgressSession.map(_.toRight(SessionService.FinishSessionError.WrongSessionStatus))

  override def stop: F[Unit] =
    currentSessionStateManager.reset
