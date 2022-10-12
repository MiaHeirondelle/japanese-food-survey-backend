package jp.ac.tachibana.food_survey.services.session

import cats.data.{EitherT, NonEmptyList, OptionT}
import cats.effect.std.Queue
import cats.effect.syntax.spawn.*
import cats.effect.syntax.monadCancel.*
import cats.syntax.option.*
import cats.effect.{Fiber, Ref, Temporal}
import cats.syntax.applicative.*
import cats.syntax.applicativeError.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.domain.user.User.Id
import jp.ac.tachibana.food_survey.persistence.session.SessionRepository
import jp.ac.tachibana.food_survey.services.session.SessionListenerService
import jp.ac.tachibana.food_survey.services.session.managers.CurrentSessionStateManager
import jp.ac.tachibana.food_survey.services.session.model.*

import scala.concurrent.duration.{Duration, FiniteDuration}

// todo: fix syntax - context bounds?
// todo: store users to check rights
class DefaultSessionListenerService[F[_]: Temporal](
  currentSessionStateManager: CurrentSessionStateManager[F],
  ticksRef: Ref[F, Option[Fiber[F, Throwable, Unit]]],
  outputsMapRef: Ref[F, Map[User.Id, Queue[F, OutputSessionMessage]]])
    extends SessionListenerService[F]:

  // todo: check session state
  override def connect[L](
    listenerBuilder: SessionListenerBuilder[F, L],
    processor: SessionMessageProcessor[F]
  )(user: User): F[Either[SessionListenerService.ConnectionError, L]] =
    OptionT(currentSessionStateManager.getCurrentSession)
      .filter(_.participants.exists(_.id === user.id))
      .semiflatMap { session =>
        for {
          output <- Queue.unbounded[F, OutputSessionMessage]
          // todo: check reconnections -> close old sockets?
          _ <- outputsMapRef.update(_.updated(user.id, output))
          _ <- broadcastMessage(OutputSessionMessage.UserJoined(user, session))(session)
          listener <- listenerBuilder(transformInput(processor, user), createOutputStream(output))
        } yield listener
      }
      .toRight(SessionListenerService.ConnectionError.InvalidSessionState)
      .value

  override def broadcast(message: OutputSessionMessage): F[Unit] =
    currentSessionStateManager.getCurrentSession.flatMap(_.traverse(broadcastMessage(message))).void

  override def isTickActive: F[Boolean] =
    ticksRef.get.map(_.nonEmpty)

  // todo: fiber stop errors
  override def tickBroadcast(
    tick: FiniteDuration,
    limit: Duration
  )(tickF: (Session, Duration) => F[Option[OutputSessionMessage]]): F[Unit] =
    val task = repeatExecution(tick, limit)(l =>
      (for {
        session <- OptionT(currentSessionStateManager.getCurrentSession)
        message <- OptionT(tickF(session, l))
        result <- OptionT.liftF(broadcastMessage(message)(session))
      } yield result).value.void)

    // todo: old tick - uncancelable!
    task.start.flatMap(fiber => ticksRef.set(fiber.some))

  override def stopTick: F[Unit] =
    ticksRef.getAndSet(none).flatMap(_.traverse(_.cancel)).void

  override def stop: F[Unit] =
    stopTick >> unregisterListeners

  private def createOutputStream(
    queue: Queue[F, OutputSessionMessage]): SessionListenerOutput[F] =
    fs2.Stream.fromQueueUnterminated(queue)

  // todo: on close unregister?
  private def transformInput(
    processor: SessionMessageProcessor[F],
    inputUser: User
  )(input: SessionListenerInput[F]): fs2.Stream[F, Unit] =
    input.evalMap { inputMessage =>
      val processF = for {
        session <- OptionT(currentSessionStateManager.getCurrentSession)
        perUserProcessor <- OptionT.liftF(processor(inputMessage, session, inputUser))
        result <- OptionT.liftF(session.participants.traverse { outputUser =>
          for {
            outputMessage <- perUserProcessor(outputUser)
            sendResult <- outputMessage.traverse(sendMessage(outputUser.id))
          } yield sendResult
        })
      } yield result
      processF.value.void
    }

  private def unregisterListeners: F[Unit] =
    broadcastMessageToAll(OutputSessionMessage.Shutdown) >> outputsMapRef.set(Map.empty)

  private def broadcastMessageToAll(message: OutputSessionMessage): F[Unit] =
    currentSessionStateManager.getCurrentSession.map(_.traverse(broadcastMessage(message)))

  private def broadcastMessage(message: OutputSessionMessage)(session: Session): F[Unit] =
    session.participants.traverse(u => sendMessage(u.id)(message)).void

  private def sendMessage(userId: User.Id)(message: OutputSessionMessage): F[Unit] =
    for {
      allOutputs <- outputsMapRef.get
      output = allOutputs.get(userId)
      _ <- output.traverse(_.offer(message))
    } yield ()

  // todo: on stop callback?
  private def repeatExecution(
    tick: FiniteDuration,
    limit: Duration
  )(task: Duration => F[Unit]): F[Unit] =
    if (limit < Duration.Zero)
      ().pure[F]
    else
      task(limit).attempt >> Temporal[F].sleep(tick) >> repeatExecution(tick, limit - tick)(task)

object DefaultSessionListenerService:

  def create[F[_]: Temporal](
    currentSessionStateManager: CurrentSessionStateManager[F]): F[SessionListenerService[F]] =
    for {
      outputsMapRef <- Ref.of(Map.empty[User.Id, Queue[F, OutputSessionMessage]])
      ticksRef <- Ref.of(none[Fiber[F, Throwable, Unit]])
    } yield new DefaultSessionListenerService[F](currentSessionStateManager, ticksRef, outputsMapRef)
