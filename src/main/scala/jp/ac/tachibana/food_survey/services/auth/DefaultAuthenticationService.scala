package jp.ac.tachibana.food_survey.services.auth

import java.time.Instant

import cats.Monad
import cats.data.OptionT
import cats.effect.{Clock, Sync}
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.monad.*
import cats.syntax.traverse.*
import cats.syntax.foldable.*
import cats.instances.option.*
import cats.syntax.applicative.*
import cats.syntax.traverseFilter.*

import jp.ac.tachibana.food_survey.domain.user.{User, UserCredentials}
import jp.ac.tachibana.food_survey.persistence.auth.{AuthTokenRepository, CredentialsRepository}
import jp.ac.tachibana.food_survey.persistence.user.UserRepository
import jp.ac.tachibana.food_survey.services.auth.domain.{AuthDetails, AuthToken, HashedUserCredentials}
import jp.ac.tachibana.food_survey.util.crypto.{CryptoHasher, Hash, TokenHasher}

class DefaultAuthenticationService[F[_]: Monad: Clock](
  cryptoHasher: CryptoHasher[F],
  tokenHasher: TokenHasher[F],
  tokenGenerator: AuthTokenGenerator[F],
  authTokenRepository: AuthTokenRepository[F],
  credentialsRepository: CredentialsRepository[F],
  userRepository: UserRepository[F])
    extends AuthenticationService[F]:

  override def saveCredentials(
    userId: User.Id,
    credentials: UserCredentials): F[Unit] =
    for {
      hashedCredentials <- cryptoHasher.hashCredentials(credentials)
      storedCredentials = CredentialsRepository.StoredCredentials.fromHashedCredentials(userId, hashedCredentials)
      result <- credentialsRepository.insert(storedCredentials)
    } yield result

  override def login(credentials: UserCredentials): F[Either[AuthenticationService.LoginError, AuthDetails]] =
    // todo: remove tokens if there are too many
    (for {
      matchingCredentials <- OptionT(credentialsRepository.getByLogin(credentials.login.value))
      result <- OptionT
        .liftF(cryptoHasher
          .verifyHash(credentials.password.value.value, matchingCredentials.passwordHash, matchingCredentials.passwordSalt))
        .ifM(
          ifTrue = OptionT(userRepository.getByCredentials(matchingCredentials.toHashedCredentials))
            .semiflatMap(user =>
              for {
                authToken <- tokenGenerator.generate
                authTokenHash <- tokenHasher.hash(authToken)
                createdAt <- Clock[F].realTime
                _ <- authTokenRepository.insert(user.id, authTokenHash, Instant.ofEpochMilli(createdAt.toMillis))
              } yield AuthDetails(authToken, user)),
          ifFalse = OptionT.none
        )
    } yield result).fold(AuthenticationService.LoginError.InvalidCredentials.asLeft)(_.asRight)

  override def authenticate(token: AuthToken): F[Either[AuthenticationService.AuthenticationError, User]] =
    (for {
      tokenHash <- OptionT.liftF(tokenHasher.hash(token))
      userId <- OptionT(authTokenRepository.get(tokenHash))
      user <- OptionT(userRepository.get(userId))
    } yield user.asRight[AuthenticationService.AuthenticationError])
    .getOrElse(Left(AuthenticationService.AuthenticationError.UserNotFound))

  override def logout(token: AuthToken): F[Unit] =
    for {
      tokenHash <- tokenHasher.hash(token)
      result <- authTokenRepository.delete(tokenHash)
    } yield result

object DefaultAuthenticationService:

  def create[F[_]: Sync](
    authTokenRepository: AuthTokenRepository[F],
    credentialsRepository: CredentialsRepository[F],
    userRepository: UserRepository[F]): F[AuthenticationService[F]] =
    for {
      cryptoHasher <- CryptoHasher.create[F]
      tokenHasher = new TokenHasher[F]
      tokenGenerator <- DefaultAuthTokenGenerator.create[F]
    } yield new DefaultAuthenticationService[F](
      cryptoHasher,
      tokenHasher,
      tokenGenerator,
      authTokenRepository,
      credentialsRepository,
      userRepository)
