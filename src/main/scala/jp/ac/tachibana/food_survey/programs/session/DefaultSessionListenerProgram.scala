package jp.ac.tachibana.food_survey.programs.session

import cats.Monad
import cats.data.{EitherT, NonEmptyList, OptionT}
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.functorFilter.*
import cats.syntax.option.*
import cats.syntax.traverse.*

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.services.session.model.*
import jp.ac.tachibana.food_survey.services.session.{SessionListenerService, SessionService}

class DefaultSessionListenerProgram[F[_]: Monad](
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
        // todo: check session only finished once
        sessionService.finish.as(OutputSessionMessage.SessionFinished(session).some)
      case SessionService.SessionElementState.Question(session, state, question) =>
        OutputSessionMessage.ElementSelected(session, question).some.pure[F]
      case SessionService.SessionElementState.Transitioning(session) =>
        none[OutputSessionMessage].pure[F]
    }
