package jp.ac.tachibana.food_survey.programs.session

import cats.Monad
import cats.data.{EitherT, NonEmptyList}
import cats.effect.std.Queue
import cats.effect.{GenConcurrent, Ref}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.domain.user.User.Id
import jp.ac.tachibana.food_survey.programs.session.*
import jp.ac.tachibana.food_survey.programs.session.SessionListenerProgram.*

// todo: fix syntax - context bounds?
// todo: store users to check rights
class WebSocketSessionListenerProgram[F[_]](
  statesMapRef: Ref[F, Map[Session.Number, Session]],
  outputsMapRef: Ref[F, Map[User.Id, Queue[F, SessionListenerProgram.OutputMessage]]],
  program: SessionProgram[F]
)(implicit F: GenConcurrent[F, ?])
    extends SessionListenerProgram[F]:

  override def create[L](
    listenerBuilder: ListenerBuilder[F, L]
  )(creator: User.Admin,
    respondents: NonEmptyList[Id]): F[Either[SessionProgram.SessionCreationError, L]] =
    EitherT(program.create(creator, respondents)).semiflatMap { session =>
      for {
        _ <- statesMapRef.update(_.updated(session.number, session))
        output <- Queue.unbounded[F, SessionListenerProgram.OutputMessage]
        _ <- outputsMapRef.update(_.updated(creator.id, output))
        listener <- listenerBuilder(transformInput(creator), createOutputStream(output))
      } yield listener
    }.value

  override def join[L](
    listenerBuilder: ListenerBuilder[F, L]
  )(respondent: User.Respondent): F[Either[SessionProgram.SessionJoinError, L]] =
    EitherT(program.join(respondent)).semiflatMap { session =>
      for {
        _ <- statesMapRef.update(_.updated(session.number, session))
        output <- Queue.unbounded[F, SessionListenerProgram.OutputMessage]
        _ <- outputsMapRef.update(_.updated(respondent.id, output))
        _ <- broadcastMessage(SessionListenerProgram.OutputMessage.RespondentJoined(respondent, session))(session)
        listener <- listenerBuilder(transformInput(respondent), createOutputStream(output))
      } yield listener
    }.value

  override def getActiveSession: F[Option[Session]] =
    program.getActiveSession

  override def stop: F[Unit] =
    program.stop >> unregisterListeners

  private def createOutputStream(queue: Queue[F, SessionListenerProgram.OutputMessage]): ListenerOutput[F] =
    fs2.Stream.fromQueueUnterminated(queue)

  // todo: on close unregister?
  private def transformInput(user: User)(input: ListenerInput[F]): fs2.Stream[F, Unit] =
    input.evalMap { case SessionListenerProgram.InputMessage.BeginSession(number) =>
      Monad[F].pure(())
    }

  private def unregisterListeners: F[Unit] =
    broadcastMessageToAll(SessionListenerProgram.OutputMessage.Shutdown) >> statesMapRef.set(Map.empty) >> outputsMapRef.set(
      Map.empty)

  private def broadcastMessageToAll(message: SessionListenerProgram.OutputMessage): F[Unit] =
    statesMapRef.get.map(_.values.toList.traverse(broadcastMessage(message)))

  private def broadcastMessage(message: SessionListenerProgram.OutputMessage)(session: Session): F[Unit] =
    session.participants.traverse(u => sendMessage(message)(u.id)).void

  private def sendMessage(message: SessionListenerProgram.OutputMessage)(userId: User.Id): F[Unit] =
    for {
      allOutputs <- outputsMapRef.get
      output = allOutputs.get(userId)
      _ <- output.traverse(_.offer(message))
    } yield ()
