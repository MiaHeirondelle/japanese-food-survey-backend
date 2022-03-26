package jp.ac.tachibana.food_survey.services.session.managers

import cats.Functor
import cats.effect.Ref
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.order.*

import jp.ac.tachibana.food_survey.domain.question.{Question, QuestionAnswer}
import jp.ac.tachibana.food_survey.domain.session.{Session, SessionAnswers, SessionElement}
import jp.ac.tachibana.food_survey.services.session.SessionService
import jp.ac.tachibana.food_survey.services.session.managers.DefaultInProgressSessionManager.*

class DefaultInProgressSessionManager[F[_]: Functor] private (ref: Ref[F, Option[DefaultInProgressSessionManager.InternalState]])
    extends InProgressSessionManager[F]:

  override def registerSession(session: Session.InProgress): F[Unit] =
    ref.set(Some(DefaultInProgressSessionManager.InternalState(usersReadyToTransition = 0, session)))

  override def getCurrentState: F[Option[SessionService.SessionElementState]] =
    ref.get
      .map(_.flatMap(state =>
        state.session.currentElement.map { case e: SessionElement.Question =>
          questionElementState(state.session, e)
        }))

  override def provideAnswer(
    answer: QuestionAnswer): F[Either[InProgressSessionManager.Error, SessionService.SessionElementState.Question]] =
    modifyNonEmpty { state =>
      state.session.currentElement match {
        case Some(e: SessionElement.Question) if e.question.id === answer.questionId =>
          val newSession = state.session.provideAnswer(answer)
          val newAnswerCount = newSession.answersCount(answer.questionId)
          val newQuestionState = questionElementState(newSession, e)

          (state.copy(usersReadyToTransition = newAnswerCount, session = newSession).some, newQuestionState.asRight)
        case Some(e: SessionElement.Question) =>
          (state.some, InProgressSessionManager.Error.IncorrectSessionState.asLeft)
        case None =>
          (state.some, InProgressSessionManager.Error.IncorrectSessionState.asLeft)
      }
    }

  override def transitionToNextElement: F[Either[InProgressSessionManager.Error, Option[SessionService.SessionElementState]]] =
    modifyNonEmpty { state =>
      val newSessionOpt = state.session.incrementCurrentElementNumber
      val nextElementStateOpt = newSessionOpt.flatMap(s => s.currentElement.map(defaultSessionElementState(s)))
      (
        newSessionOpt.fold(state)(state.copy(usersReadyToTransition = 0, _)).some,
        nextElementStateOpt.asRight
      )
    }

  override def unregisterSession: F[Unit] =
    ref.set(None)

  private def modifyNonEmpty[A](
    f: DefaultInProgressSessionManager.InternalState => DefaultInProgressSessionManager.StateTransformationResult[A])
    : F[DefaultInProgressSessionManager.OperationResult[A]] =
    ref.modify(stateOpt => stateOpt.fold((stateOpt, InProgressSessionManager.Error.NoSessionInProgress.asLeft[A]))(f))

object DefaultInProgressSessionManager:

  private type OperationResult[A] =
    Either[InProgressSessionManager.Error, A]

  private type StateTransformationResult[A] =
    (Option[DefaultInProgressSessionManager.InternalState], OperationResult[A])

  private case class InternalState(
    usersReadyToTransition: Int,
    session: Session.InProgress)

  private def defaultSessionElementState(
    session: Session.InProgress
  )(sessionElement: SessionElement): SessionService.SessionElementState =
    sessionElement match {
      case e: SessionElement.Question =>
        SessionService.SessionElementState.Question(session, SessionService.QuestionState.Pending, e)
    }

  private def questionElementState(
    session: Session.InProgress,
    questionElement: SessionElement.Question): SessionService.SessionElementState.Question =
    val questionState =
      if (session.isQuestionAnswered(questionElement.question.id))
        SessionService.QuestionState.Finished
      else
        SessionService.QuestionState.Pending
    SessionService.SessionElementState.Question(session, questionState, questionElement)
