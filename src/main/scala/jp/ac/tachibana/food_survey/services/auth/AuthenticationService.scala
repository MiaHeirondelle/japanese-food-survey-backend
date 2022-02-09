package jp.ac.tachibana.food_survey.services.auth

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.services.auth.domain.AuthToken

trait AuthenticationService[F[_]]:

  def login(
    username: String,
    password: String): F[Either[AuthenticationService.LoginError, AuthToken]]

  def authenticate(token: AuthToken): F[Either[AuthenticationService.AuthenticationError, User]]

  def logout(token: AuthToken): F[Unit]

object AuthenticationService:

  sealed trait LoginError

  object LoginError:
    case object InvalidCredentials extends AuthenticationService.LoginError

  sealed trait AuthenticationError

  object AuthenticationError:
    case object UserNotFound extends AuthenticationService.AuthenticationError
    case object SessionExpired extends AuthenticationService.AuthenticationError
