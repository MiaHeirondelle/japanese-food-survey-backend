package jp.ac.tachibana.food_survey.services.session.managers

import cats.Functor
import cats.data.NonEmptySet
import cats.effect.{Ref, Sync}
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.order.*

import jp.ac.tachibana.food_survey.domain.question.{Question, QuestionAnswer}
import jp.ac.tachibana.food_survey.domain.session.{Session, SessionAnswers, SessionElement}
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.services.session.SessionService
import jp.ac.tachibana.food_survey.services.session.managers.DefaultInProgressSessionManager.*

class DefaultInProgressSessionManager[F[_]: Functor] private (ref: Ref[F, Option[DefaultInProgressSessionManager.InternalState]])
    extends InProgressSessionManager[F]:

  // todo: prevent registering if already registered
  override private[managers] def registerSession(session: Session.InProgress): F[Unit] =
    ref.set(
      DefaultInProgressSessionManager
        .InternalState(
          DefaultInProgressSessionManager.TransitionState.CurrentElement(Set.empty),
          session
        )
        .some
    )

  override def getCurrentState: F[Option[SessionService.SessionElementState]] =
    ref.get.map(_.map(currentElementState))

  override def provideAnswer(
    answer: QuestionAnswer): F[Either[InProgressSessionManager.Error, SessionService.SessionElementState.Question]] =
    modifyNonEmpty(state =>
      mapInProgressSession(state)(session =>
        session.currentElement match {
          case e: SessionElement.Question
              if e.question.id === answer.questionId && session.joinedUsers.exists(_.id === answer.respondentId) =>
            val newSession = session.provideAnswer(answer)
            val newAnswerCount = newSession.answersCount(answer.questionId)
            val newQuestionState = questionElementState(newSession, e)

            (state.copy(session = newSession).some, newQuestionState.asRight)
          case e: SessionElement.Question =>
            errorStateTransformationResult(state)
        }))

  override def transitionToNextElement: F[Either[InProgressSessionManager.Error, SessionService.NonPendingSessionElementState]] =
    modifyNonEmpty(state => mapInProgressSession(state)(transitionSessionToNextElementState))

  override def transitionToNextElement(
    respondentId: User.Id): F[Either[InProgressSessionManager.Error, SessionService.SessionElementState]] =
    modifyNonEmpty(state =>
      mapInProgressSession(state) { session =>
        state.transition match {
          case t: DefaultInProgressSessionManager.TransitionState.CurrentElement =>
            val transition = t.copy(t.usersReadyToTransition + respondentId)
            if (session.joinedUsers.forall(r => transition.usersReadyToTransition.contains(r.id)))
              val newState = state.copy(transition = DefaultInProgressSessionManager.TransitionState.NotTransitioning)
              (newState.some, currentElementState(newState).asRight)
            else
              val newState = state.copy(transition)
              (newState.some, currentElementState(newState).asRight)

          case t: DefaultInProgressSessionManager.TransitionState.NextElement =>
            val transition = t.copy(t.usersReadyToTransition.add(respondentId))
            if (session.joinedUsers.forall(r => transition.usersReadyToTransition.contains(r.id)))
              transitionSessionToNextElementState(session)
            else
              val newState = state.copy(transition)
              (newState.some, currentElementState(newState).asRight)

          case DefaultInProgressSessionManager.TransitionState.NotTransitioning =>
            val transition = DefaultInProgressSessionManager.TransitionState.NextElement(NonEmptySet.one(respondentId))
            if (session.joinedUsers.forall(r => transition.usersReadyToTransition.contains(r.id)))
              transitionSessionToNextElementState(session)
            else
              val newState = state.copy(transition)
              (newState.some, currentElementState(newState).asRight)
        }
      })

  private def transitionSessionToNextElementState(
    session: Session.InProgress): StateTransformationResult[SessionService.NonPendingSessionElementState] =
    session.incrementCurrentElementNumber.getOrElse(Session.Finished.fromInProgress(session)) match {
      case inProgress: Session.InProgress =>
        val newState = DefaultInProgressSessionManager.InternalState(
          DefaultInProgressSessionManager.TransitionState.NotTransitioning,
          inProgress
        )
        val elementState = nonPendingCurrentSessionElementState(inProgress)
        (newState.some, elementState.asRight)

      case finished: Session.Finished =>
        val newState = DefaultInProgressSessionManager.InternalState(
          DefaultInProgressSessionManager.TransitionState.NotTransitioning,
          finished
        )
        val elementState = SessionService.SessionElementState.Finished(finished)
        (newState.some, elementState.asRight)
    }

  override private[managers] def unregisterSession: F[Unit] =
    ref.set(None)

  private def modifyNonEmpty[A](
    f: DefaultInProgressSessionManager.InternalState => DefaultInProgressSessionManager.StateTransformationResult[A])
    : F[DefaultInProgressSessionManager.OperationResult[A]] =
    ref.modify(stateOpt => stateOpt.fold((stateOpt, InProgressSessionManager.Error.IncorrectSessionState.asLeft[A]))(f))

object DefaultInProgressSessionManager:

  def create[F[_]: Sync]: F[InProgressSessionManager[F]] =
    Ref.of[F, Option[DefaultInProgressSessionManager.InternalState]](None).map(new DefaultInProgressSessionManager(_))

  private type OperationResult[A] =
    Either[InProgressSessionManager.Error, A]

  private type StateTransformationResult[A] =
    (Option[DefaultInProgressSessionManager.InternalState], OperationResult[A])

  sealed private trait TransitionState

  private object TransitionState:
    case class CurrentElement(usersReadyToTransition: Set[User.Id]) extends DefaultInProgressSessionManager.TransitionState
    case class NextElement(usersReadyToTransition: NonEmptySet[User.Id]) extends DefaultInProgressSessionManager.TransitionState
    case object NotTransitioning extends DefaultInProgressSessionManager.TransitionState

  private case class InternalState(
    transition: DefaultInProgressSessionManager.TransitionState,
    session: Session.InProgressOrFinished)

  private def currentElementState(state: DefaultInProgressSessionManager.InternalState): SessionService.SessionElementState =
    state.session match {
      case inProgress: Session.InProgress =>
        state.transition match {
          case _: DefaultInProgressSessionManager.TransitionState.CurrentElement =>
            SessionService.SessionElementState.Transitioning(inProgress)

          case _: DefaultInProgressSessionManager.TransitionState.NextElement =>
            SessionService.SessionElementState.Transitioning(inProgress)

          case DefaultInProgressSessionManager.TransitionState.NotTransitioning =>
            nonPendingCurrentSessionElementState(inProgress)
        }
      case finished: Session.Finished =>
        SessionService.SessionElementState.Finished(finished)
    }

  private def nonPendingCurrentSessionElementState(session: Session.InProgress): SessionService.NonPendingSessionElementState =
    session.currentElement match {
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

  private def errorStateTransformationResult[A](state: InternalState): StateTransformationResult[A] =
    (state.some, InProgressSessionManager.Error.IncorrectSessionState.asLeft[A])

  private def mapInProgressSession[A](
    state: DefaultInProgressSessionManager.InternalState
  )(f: Session.InProgress => StateTransformationResult[A]): StateTransformationResult[A] =
    state.session match {
      case session: Session.InProgress =>
        f(session)
      case _: Session.Finished =>
        errorStateTransformationResult(state)
    }
