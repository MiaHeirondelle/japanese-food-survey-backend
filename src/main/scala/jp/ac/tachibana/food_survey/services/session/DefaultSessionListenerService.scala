package jp.ac.tachibana.food_survey.services.session

import cats.Monad
import cats.data.{EitherT, NonEmptyList, OptionT}
import cats.effect.std.Queue
import cats.effect.{GenConcurrent, Ref}
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.domain.user.User.Id
import jp.ac.tachibana.food_survey.persistence.session.SessionRepository
import jp.ac.tachibana.food_survey.services.session.SessionListenerService

// todo: fix syntax - context bounds?
// todo: store users to check rights
class DefaultSessionListenerService[F[_]](
  sessionRepository: SessionRepository[F],
  outputsMapRef: Ref[F, Map[User.Id, Queue[F, SessionListenerService.OutputMessage]]]
)(implicit F: GenConcurrent[F, ?])
    extends SessionListenerService[F]:

  override def connect[L](
    listenerBuilder: SessionListenerService.ListenerBuilder[F, L],
    processor: SessionListenerService.MessageProcessor[F]
  )(user: User): F[Either[SessionListenerService.ConnectionError, L]] =
    OptionT(sessionRepository.getActiveSession)
      .filter(_.participants.exists(_.id === user.id))
      .semiflatMap { session =>
        for {
          output <- Queue.unbounded[F, SessionListenerService.OutputMessage]
          // todo: check reconnections -> close old sockets?
          _ <- outputsMapRef.update(_.updated(user.id, output))
          _ <- broadcastMessage(SessionListenerService.OutputMessage.UserJoined(user, session))(session)
          listener <- listenerBuilder(transformInput(processor, user), createOutputStream(output))
        } yield listener
      }
      .toRight(SessionListenerService.ConnectionError.InvalidSessionState)
      .value

  override def stop: F[Unit] =
    unregisterListeners

  private def createOutputStream(
    queue: Queue[F, SessionListenerService.OutputMessage]): SessionListenerService.ListenerOutput[F] =
    fs2.Stream.fromQueueUnterminated(queue)

  // todo: on close unregister?
  private def transformInput(
    processor: SessionListenerService.MessageProcessor[F],
    user: User
  )(input: SessionListenerService.ListenerInput[F]): fs2.Stream[F, Unit] =
    input.evalMap { inputMessage =>
      val processF = for {
        session <- OptionT(sessionRepository.getActiveSession)
        outputMessage <- OptionT(processor(inputMessage, session, user))
        result <- OptionT.liftF(broadcastMessage(outputMessage)(session))
      } yield result
      processF.value.void
    }

  private def unregisterListeners: F[Unit] =
    broadcastMessageToAll(SessionListenerService.OutputMessage.Shutdown) >> outputsMapRef.set(Map.empty)

  private def broadcastMessageToAll(message: SessionListenerService.OutputMessage): F[Unit] =
    sessionRepository.getActiveSession.map(_.traverse(broadcastMessage(message)))

  private def broadcastMessage(message: SessionListenerService.OutputMessage)(session: Session): F[Unit] =
    session.participants.traverse(u => sendMessage(message)(u.id)).void

  private def sendMessage(message: SessionListenerService.OutputMessage)(userId: User.Id): F[Unit] =
    for {
      allOutputs <- outputsMapRef.get
      output = allOutputs.get(userId)
      _ <- output.traverse(_.offer(message))
    } yield ()

object DefaultSessionListenerService:

  def create[F[_]](sessionRepository: SessionRepository[F])(implicit F: GenConcurrent[F, ?]): F[SessionListenerService[F]] =
    for {
      outputsMapRef <- Ref.of(Map.empty[User.Id, Queue[F, SessionListenerService.OutputMessage]])
    } yield new DefaultSessionListenerService[F](sessionRepository, outputsMapRef)
