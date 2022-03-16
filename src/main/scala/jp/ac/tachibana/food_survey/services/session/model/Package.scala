package jp.ac.tachibana.food_survey.services.session.model

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User

type SessionMessageProcessor[F[_]] =
  (InputSessionMessage, Session, User) => F[Option[OutputSessionMessage]]

type SessionListenerInput[F[_]] =
  fs2.Stream[F, InputSessionMessage]

type SessionListenerOutput[F[_]] =
  fs2.Stream[F, OutputSessionMessage]

type SessionListenerInputTransformer[F[_]] =
  SessionListenerInput[F] => fs2.Stream[F, Unit]

type SessionListenerBuilder[F[_], L] =
  (SessionListenerInputTransformer[F], SessionListenerOutput[F]) => F[L]
