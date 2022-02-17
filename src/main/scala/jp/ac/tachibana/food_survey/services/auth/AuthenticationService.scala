package jp.ac.tachibana.food_survey.services.auth

import jp.ac.tachibana.food_survey.domain.user.{User, UserCredentials}
import jp.ac.tachibana.food_survey.services.auth.domain.{AuthDetails, AuthToken, HashedUserCredentials}

trait AuthenticationService[F[_]]:

  def saveCredentials(
    userId: User.Id,
    credentials: UserCredentials): F[Unit]

  def login(credentials: UserCredentials): F[Either[AuthenticationService.LoginError, AuthDetails]]

  def authenticate(token: AuthToken): F[Either[AuthenticationService.AuthenticationError, User]]

  def logout(token: AuthToken): F[Unit]

object AuthenticationService:

  sealed trait LoginError

  sealed trait AuthenticationError

  object LoginError:
    case object InvalidCredentials extends AuthenticationService.LoginError

  object AuthenticationError:
    case object UserNotFound extends AuthenticationService.AuthenticationError
    case object SessionExpired extends AuthenticationService.AuthenticationError
