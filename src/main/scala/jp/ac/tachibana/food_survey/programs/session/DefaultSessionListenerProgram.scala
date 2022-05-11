package jp.ac.tachibana.food_survey.programs.session

import cats.Monad
import cats.data.{EitherT, NonEmptyList, OptionT}
import cats.effect.Concurrent
import cats.effect.syntax.spawn.*
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.functorFilter.*
import cats.syntax.option.*
import cats.syntax.traverse.*
import jp.ac.tachibana.food_survey.domain.question.{Question, QuestionAnswer}
import jp.ac.tachibana.food_survey.domain.session.{Session, SessionElement}
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.services.session.model.*
import jp.ac.tachibana.food_survey.services.session.{SessionListenerService, SessionService}

import scala.concurrent.duration.*

class DefaultSessionListenerProgram[F[_]: Concurrent](
  sessionService: SessionService[F],
  sessionListenerService: SessionListenerService[F])
    extends SessionListenerProgram[F]:

  override def connect[L](
    listenerBuilder: SessionListenerBuilder[F, L]
  )(user: User): F[Either[SessionListenerProgram.ConnectionError, L]] =
    sessionListenerService
      .connect(listenerBuilder, processMessage)(user)
      .map(_.left.map { case SessionListenerService.ConnectionError.InvalidSessionState =>
        SessionListenerProgram.ConnectionError.InvalidSessionState
      })

  override def stop: F[Unit] =
    sessionListenerService.stop

  // todo: bug when a user refreshes the page and he's immediately ready for the next element
  // rejoin message that only sent on refresh that doesn't transition if session in progress
  private def processMessage(
    message: InputSessionMessage,
    session: Session,
    user: User): F[Option[OutputSessionMessage]] =
    message match {
      case InputSessionMessage.BeginSession =>
        OptionT(sessionService.begin(session.admin).map(_.toOption))
          .map[OutputSessionMessage](OutputSessionMessage.SessionBegan.apply)
          .value
      case InputSessionMessage.ReadyToProceed =>
        user match {
          case _: User.Admin =>
            EitherT(sessionService.getCurrentElementState).toOption.map {
              case s: SessionService.SessionElementState.Finished =>
                OutputSessionMessage.SessionFinished(s.session)
              case s: SessionService.SessionElementState.Question =>
                elementSelectedMessage(s.session)
              case s: SessionService.SessionElementState.QuestionReview =>
                elementSelectedMessage(s.session)
              case s: SessionService.SessionElementState.Text =>
                elementSelectedMessage(s.session)
              case s: SessionService.SessionElementState.BeforeFirstElement =>
                elementSelectedMessage(s.session)
              case s: SessionService.SessionElementState.Paused =>
                OutputSessionMessage.SessionPaused
            }.value

          case _: User.Respondent =>
            EitherT(sessionService.getCurrentElementState).toOption.flatMapF {
              case _: SessionService.SessionElementState.BeforeFirstElement =>
                EitherT(sessionService.transitionToFirstElement(user.id)).toOption
                  .flatMapF(processElementState)
                  .value

              case state: SessionService.NonPendingSessionElementState =>
                state.session match {
                  case s: Session.InProgress =>
                    elementSelectedMessage(s).some.pure[F]

                  case s: Session.Finished =>
                    OutputSessionMessage.SessionFinished(s).some.pure[F]
                }

              case _: SessionService.SessionElementState.Paused =>
                OutputSessionMessage.SessionPaused.some.pure[F]
            }.value
        }
      case InputSessionMessage.PauseSession =>
        user match {
          case _: User.Respondent =>
            none.pure[F]
          case _: User.Admin =>
            EitherT(sessionService.pause)
              .semiflatTap(_ => sessionListenerService.stopTick)
              .value
              .map(_.toOption.as(OutputSessionMessage.SessionPaused))
        }

      case InputSessionMessage.ResumeSession =>
        user match {
          case _: User.Respondent =>
            none.pure[F]
          case _: User.Admin =>
            EitherT(sessionService.resume)
              .semiflatMap(processNonPendingElementState)
              .value
              .map(_.toOption)
        }

      case InputSessionMessage.ProvideIntermediateAnswer(questionId, scaleValue, comment) =>
        session match {
          case s: Session.InProgress =>
            OptionT
              .fromOption(s.questionById(questionId))
              .flatMap { question =>
                val answer = question.toAnswer(session.number, user.id, scaleValue, comment)
                EitherT(sessionService.provideAnswer(answer)).toOption
              }
              .value
              .as(none)

          case _ =>
            none[OutputSessionMessage].pure[F]
        }

      case InputSessionMessage.ProvideAnswer(questionId, scaleValue, comment) =>
        session match {
          case s: Session.InProgress =>
            OptionT
              .fromOption(s.questionById(questionId))
              .flatMap { question =>
                val answer = question.toAnswer(session.number, user.id, scaleValue, comment)
                EitherT(sessionService.provideAnswer(answer)).toOption
              }
              .flatMap {
                case SessionService.SessionElementState.Question(_, SessionService.QuestionState.Finished, _) =>
                  OptionT(forceProceedToNextElement)

                case SessionService.SessionElementState.Question(_, SessionService.QuestionState.Pending, _) =>
                  OptionT.none
              }
              .value

          case _ =>
            none[OutputSessionMessage].pure[F]
        }
    }

  private def proceedToNextElement: F[Option[OutputSessionMessage]] =
    EitherT(sessionService.transitionToNextElement).toOption
      .semiflatMap(processNonPendingElementState)
      .value

  private def forceProceedToNextElement: F[Option[OutputSessionMessage]] =
    EitherT(sessionService.transitionToNextElement).toOption
      .semiflatMap(state => sessionListenerService.stopTick >> processNonPendingElementState(state))
      .value

  private def processNonPendingElementState(state: SessionService.NonPendingSessionElementState): F[OutputSessionMessage] =
    state match {
      case SessionService.SessionElementState.Finished(session) =>
        // todo: check session only finished once
        sessionService.finish.as(OutputSessionMessage.SessionFinished(session))
      case SessionService.SessionElementState.Question(session, state, question) =>
        startSessionElementTimerTicks(question.showDuration)
          .as(questionElementSelectedMessage(session, question))
      case SessionService.SessionElementState.QuestionReview(session, questionReview) =>
        startSessionElementTimerTicks(questionReview.showDuration)
          .as(questionReviewElementSelectedMessage(session, questionReview))
      case SessionService.SessionElementState.Text(_, text) =>
        startSessionElementTimerTicks(text.showDuration).as(textElementSelectedMessage(text))
    }

  private def processElementState(state: SessionService.SessionElementState): F[Option[OutputSessionMessage]] =
    state match {
      case e: SessionService.NonPendingSessionElementState =>
        processNonPendingElementState(e).map(_.some)

      case _: SessionService.SessionElementState.BeforeFirstElement =>
        none.pure[F]

      case _: SessionService.SessionElementState.Paused =>
        none.pure[F]
    }

  private def startSessionElementTimerTicks(duration: FiniteDuration): F[Unit] =
    sessionListenerService.tickBroadcast(1.second, duration) {
      case (s, p: FiniteDuration) if p > Duration.Zero =>
        OutputSessionMessage.TimerTick(p.toMillis).some.pure[F]

      case (s: Session.InProgress, p) if p <= Duration.Zero =>
        proceedToNextElement

      case (_, _) =>
        none[OutputSessionMessage].pure[F]
    }

  private def questionElementSelectedMessage(
    session: Session.InProgress,
    question: SessionElement.Question): OutputSessionMessage =
    question match {
      case e: SessionElement.Question.Basic =>
        OutputSessionMessage.BasicQuestionSelected(e)

      case e: SessionElement.Question.Repeated =>
        OutputSessionMessage.RepeatedQuestionSelected(e, session.allAnswersForQuestion(e.previousQuestion.id))
    }

  private def questionReviewElementSelectedMessage(
    session: Session.InProgress,
    questionReview: SessionElement.QuestionReview): OutputSessionMessage =
    questionReview match {
      case e: SessionElement.QuestionReview.Basic =>
        // todo: comment about answers
        val answers = session.allAnswersForQuestion(e.question.id).collect { case a: QuestionAnswer.Basic => a }
        OutputSessionMessage.BasicQuestionReviewSelected(e, answers)

      case e: SessionElement.QuestionReview.Repeated =>
        // todo: comment about answers
        val answers = session.allAnswersForQuestion(e.question.id).collect { case a: QuestionAnswer.Repeated => a }
        val previousAnswers = session.allAnswersForQuestion(e.previousQuestion.id)
        OutputSessionMessage.RepeatedQuestionReviewSelected(e, answers, previousAnswers)
    }

  private def textElementSelectedMessage(text: SessionElement.Text): OutputSessionMessage =
    OutputSessionMessage.TextSelected(text)

  private def elementSelectedMessage(session: Session.InProgress): OutputSessionMessage =
    session.currentElement match {
      case question: SessionElement.Question =>
        questionElementSelectedMessage(session, question)
      case questionReview: SessionElement.QuestionReview =>
        questionReviewElementSelectedMessage(session, questionReview)
      case text: SessionElement.Text =>
        textElementSelectedMessage(text)
    }
