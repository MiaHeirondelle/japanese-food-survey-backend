package jp.ac.tachibana.food_survey.services.auth

import java.time.Instant

import cats.Monad
import cats.effect.{Clock, Sync}
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.persistence.auth.AuthTokenRepository
import jp.ac.tachibana.food_survey.services.auth.domain.AuthToken
import jp.ac.tachibana.food_survey.util.crypto.{CryptoHasher, Hash, TokenHasher}

class DefaultAuthenticationService[F[_]: Monad: Clock](
  cryptoHasher: CryptoHasher[F],
  tokenHasher: TokenHasher[F],
  tokenGenerator: AuthTokenGenerator[F],
  authTokenRepository: AuthTokenRepository[F])
    extends AuthenticationService[F]:

  override def login(
    username: String,
    password: String): F[Either[AuthenticationService.LoginError, AuthToken]] =
    // todo: fetch user by user id
    // todo: remove tokens if there are too many
    for {
      authToken <- tokenGenerator.generate
      authTokenHash <- tokenHasher.hash(authToken)
      createdAt <- Clock[F].realTime
      _ <- authTokenRepository.save(User.Id("test"), authTokenHash, Instant.ofEpochMilli(createdAt.toMillis))
    } yield authToken.asRight

  override def authenticate(token: AuthToken): F[Either[AuthenticationService.AuthenticationError, User.Id]] =
    for {
      tokenHash <- tokenHasher.hash(token)
      userId <- authTokenRepository.load(tokenHash)
    } yield userId.toRight(AuthenticationService.AuthenticationError.UserNotFound)

  override def logout(token: AuthToken): F[Unit] =
    for {
      tokenHash <- tokenHasher.hash(token)
      result <- authTokenRepository.remove(tokenHash)
    } yield result

object DefaultAuthenticationService:

  def create[F[_]: Sync](authTokenRepository: AuthTokenRepository[F]): F[AuthenticationService[F]] =
    for {
      cryptoHasher <- CryptoHasher.create[F]
      tokenHasher = new TokenHasher[F]
      tokenGenerator <- DefaultAuthTokenGenerator.create[F]
    } yield new DefaultAuthenticationService[F](cryptoHasher, tokenHasher, tokenGenerator, authTokenRepository)
