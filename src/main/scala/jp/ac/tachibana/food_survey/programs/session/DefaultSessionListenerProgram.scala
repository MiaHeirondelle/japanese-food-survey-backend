package jp.ac.tachibana.food_survey.programs.session

import cats.Monad
import cats.data.{EitherT, NonEmptyList, OptionT}
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
import jp.ac.tachibana.food_survey.services.session.model.*

// todo: fix syntax - context bounds?
// todo: store users to check rights
class DefaultSessionListenerProgram[F[_]](
  statesMapRef: Ref[F, Map[Session.Number, Session]],
  outputsMapRef: Ref[F, Map[User.Id, Queue[F, OutputSessionMessage]]],
  program: SessionProgram[F]
)(implicit F: GenConcurrent[F, ?])
    extends SessionListenerProgram[F]:

  override def connect[L](
    listenerBuilder: SessionListenerBuilder[F, L]
  )(user: User): F[Either[SessionListenerProgram.ConnectionError, L]] = ???

  override def stop: F[Unit] = ???

object DefaultSessionListenerProgram:

  def create[F[_]](program: SessionProgram[F])(implicit F: GenConcurrent[F, ?]): F[SessionListenerProgram[F]] =
    for {
      statesMapRef <- Ref.of(Map.empty[Session.Number, Session])
      outputsMapRef <- Ref.of(Map.empty[User.Id, Queue[F, OutputSessionMessage]])
    } yield new DefaultSessionListenerProgram[F](statesMapRef, outputsMapRef, program)
