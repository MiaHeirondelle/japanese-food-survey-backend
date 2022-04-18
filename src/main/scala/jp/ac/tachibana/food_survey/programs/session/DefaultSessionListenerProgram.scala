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
      case InputSessionMessage.ReadyForNextElement =>
        user match {
          case respondent: User.Respondent =>
            EitherT(sessionService.transitionToNextElement(respondent.id))
              .semiflatMap(processTransitionToNextElementResult)
              .toOption
              .flattenOption
              .value
          case admin: User.Admin =>
            // todo: immediate transition
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
                  EitherT(sessionService.transitionToNextElement).toOption.flatMapF(processTransitionToNextElementResult)

                case SessionService.SessionElementState.Question(_, SessionService.QuestionState.Pending, _) =>
                  OptionT.none
              }
              .value

          case _ =>
            none[OutputSessionMessage].pure[F]
        }
    }

  private def processTransitionToNextElementResult(
    elementState: SessionService.SessionElementState): F[Option[OutputSessionMessage]] =
    elementState match {
      case SessionService.SessionElementState.Finished(session) =>
        sessionListenerService.stopTicks >>
          // todo: check session only finished once
          sessionService.finish.as(OutputSessionMessage.SessionFinished(session).some)
      case SessionService.SessionElementState.Question(session, state, question) =>
        sessionListenerService.stopTicks >>
          startSessionElementTimerTicks(question) >>
          OutputSessionMessage.QuestionSelected(question).some.pure[F]
      case SessionService.SessionElementState.QuestionReview(session, questionReview) =>
        startSessionElementTimerTicks(questionReview) >>
          questionReviewElementSelectedMessage(session, questionReview).some.pure[F]
      case SessionService.SessionElementState.Transitioning(session) =>
        none[OutputSessionMessage].pure[F]
    }

  private def startSessionElementTimerTicks(element: SessionElement): F[Unit] =
    import scala.concurrent.duration.*
    sessionListenerService.tickBroadcast(1.second, element.showDuration) {
      case p: FiniteDuration if p > Duration.Zero =>
        OutputSessionMessage.TimerTick(p.toMillis)

      case _ =>
        OutputSessionMessage.TransitionToNextElement
    }

  private def questionElementSelectedMessage(question: SessionElement.Question): OutputSessionMessage =
    OutputSessionMessage.QuestionSelected(question)

  private def questionReviewElementSelectedMessage(
    session: Session.InProgress,
    questionReview: SessionElement.QuestionReview): OutputSessionMessage =
    questionReview match {
      case e: SessionElement.QuestionReview.Basic =>
        // todo: comment about answers
        val answers = session.allAnswersForQuestion(e.question.id).collect { case a: QuestionAnswer.Basic => a }
        OutputSessionMessage.BasicQuestionReviewSelected(e, answers)

      case e =>
        ???
    }
