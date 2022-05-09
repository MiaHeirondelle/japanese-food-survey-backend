package jp.ac.tachibana.food_survey.programs.auth

import cats.Functor
import cats.syntax.functor.*
import jp.ac.tachibana.food_survey.domain.auth.{AuthDetails, AuthToken}
import jp.ac.tachibana.food_survey.domain.user.{User, UserCredentials}
import jp.ac.tachibana.food_survey.services.auth.AuthenticationService

class DefaultAuthenticationProgram[F[_]: Functor](authenticationService: AuthenticationService[F])
    extends AuthenticationProgram[F]:

  override def saveCredentials(
    userId: User.Id,
    credentials: UserCredentials): F[Unit] =
    authenticationService.saveCredentials(userId, credentials)

  override def login(credentials: UserCredentials): F[Either[AuthenticationProgram.LoginError, AuthDetails]] =
    authenticationService
      .login(credentials)
      .map(_.left.map { case AuthenticationService.LoginError.InvalidCredentials =>
        AuthenticationProgram.LoginError.InvalidCredentials
      })

  override def authenticate(token: AuthToken): F[Either[AuthenticationProgram.AuthenticationError, AuthDetails]] =
    authenticationService
      .authenticate(token)
      .map(_.left.map {
        case AuthenticationService.AuthenticationError.UserNotFound =>
          AuthenticationProgram.AuthenticationError.UserNotFound
        case AuthenticationService.AuthenticationError.SessionExpired =>
          AuthenticationProgram.AuthenticationError.SessionExpired
      })

  override def logout(token: AuthToken): F[Unit] =
    authenticationService.logout(token)
