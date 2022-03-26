package jp.ac.tachibana.food_survey.services.session

import cats.effect.Ref
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.order.*
import cats.{Applicative, Monad}

import jp.ac.tachibana.food_survey.domain.question.{Question, QuestionAnswer}
import jp.ac.tachibana.food_survey.domain.session.{Session, SessionAnswers, SessionElement}
import jp.ac.tachibana.food_survey.services.session.ActiveSessionService.SessionElementState
import jp.ac.tachibana.food_survey.services.session.DefaultActiveSessionService.{defaultSessionElementState, questionState}

class DefaultActiveSessionService[F[_]: Monad] private (ref: Ref[F, Option[DefaultActiveSessionService.InternalState]])
    extends ActiveSessionService[F]:

  // todo: multiple sessions
  override def registerSession(session: Session.InProgress): F[Unit] =
    ref.set(Some(DefaultActiveSessionService.InternalState(usersReadyToTransition = 0, session)))

  override def currentEntry(sessionNumber: Session.Number): F[Either[ActiveSessionService.Error, Option[SessionElementState]]] =
    ref.get
      .map(
        _.toRight(ActiveSessionService.Error.NoSessionInProgress)
          .map(s =>
            s.session.currentElement.map { case e: SessionElement.Question =>
              questionState(s.session, e)
            })
      )

  override def provideAnswer(
    answer: QuestionAnswer): F[Either[ActiveSessionService.Error, ActiveSessionService.SessionElementState.Question]] =
    modifyNonEmpty { state =>
      state.session.currentElement match {
        case Some(e: SessionElement.Question) if e.question.id === answer.questionId =>
          val newSession = state.session.provideAnswer(answer)
          val newAnswerCount = newSession.answersCount(answer.questionId)
          val newQuestionState = questionState(newSession, e)

          (state.copy(usersReadyToTransition = newAnswerCount, session = newSession).some, newQuestionState.asRight)
        case Some(e: SessionElement.Question) =>
          (state.some, ActiveSessionService.Error.IncorrectSessionState.asLeft)
        case None =>
          (state.some, ActiveSessionService.Error.IncorrectSessionState.asLeft)
      }
    }

  override def transitionToNextElement(
    sessionNumber: Session.Number): F[Either[ActiveSessionService.Error, Option[SessionElementState]]] =
    modifyNonEmpty { state =>
      val newSession = state.session.incrementCurrentElementNumber
      val nextElement = newSession.flatMap(_.currentElement)
      (
        newSession.map(state.copy(usersReadyToTransition = 0, _)),
        nextElement.map(defaultSessionElementState).asRight
      )
    }

  // todo: multiple sessions
  override def unregisterSession(sessionNumber: Session.Number): F[Unit] =
    ref.set(None)

  private def modifyNonEmpty[A](
    f: DefaultActiveSessionService.InternalState => DefaultActiveSessionService.StateTransformationResult[A])
    : F[DefaultActiveSessionService.OperationResult[A]] =
    ref.modify(stateOpt => stateOpt.fold((stateOpt, ActiveSessionService.Error.NoSessionInProgress.asLeft[A]))(f))

object DefaultActiveSessionService:

  private type OperationResult[A] =
    Either[ActiveSessionService.Error, A]

  private type StateTransformationResult[A] =
    (Option[DefaultActiveSessionService.InternalState], OperationResult[A])

  private case class InternalState(
    usersReadyToTransition: Int,
    session: Session.InProgress)

  private def defaultSessionElementState(sessionElement: SessionElement): ActiveSessionService.SessionElementState =
    sessionElement match {
      case e: SessionElement.Question =>
        ActiveSessionService.SessionElementState.Question(ActiveSessionService.QuestionState.Pending, e)
    }

  private def questionState(
    session: Session.InProgress,
    questionElement: SessionElement.Question): ActiveSessionService.SessionElementState.Question =
    val questionState =
      if (session.isQuestionAnswered(questionElement.question.id))
        ActiveSessionService.QuestionState.Finished
      else
        ActiveSessionService.QuestionState.Pending
    ActiveSessionService.SessionElementState.Question(questionState, questionElement)
