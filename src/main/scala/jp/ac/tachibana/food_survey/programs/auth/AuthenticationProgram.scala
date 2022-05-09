package jp.ac.tachibana.food_survey.programs.auth

import jp.ac.tachibana.food_survey.domain.auth.{AuthDetails, AuthToken}
import jp.ac.tachibana.food_survey.domain.user.{User, UserCredentials}

trait AuthenticationProgram[F[_]]:

  def saveCredentials(
    userId: User.Id,
    credentials: UserCredentials): F[Unit]

  def login(credentials: UserCredentials): F[Either[AuthenticationProgram.LoginError, AuthDetails]]

  def authenticate(token: AuthToken): F[Either[AuthenticationProgram.AuthenticationError, User]]

  def logout(token: AuthToken): F[Unit]

object AuthenticationProgram:

  sealed trait LoginError

  sealed trait AuthenticationError

  object LoginError:
    case object InvalidCredentials extends AuthenticationProgram.LoginError

  object AuthenticationError:
    case object UserNotFound extends AuthenticationProgram.AuthenticationError
    case object SessionExpired extends AuthenticationProgram.AuthenticationError
