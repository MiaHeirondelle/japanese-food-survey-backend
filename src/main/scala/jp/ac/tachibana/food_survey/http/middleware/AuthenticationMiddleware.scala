package jp.ac.tachibana.food_survey.http.middleware

import cats.Monad
import cats.data.{Kleisli, OptionT}
import cats.instances.either.*
import cats.instances.option.*
import cats.syntax.bifunctor.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import org.http4s.dsl.Http4sDslBinCompat
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, Request, Response, ResponseCookie, SameSite}

import jp.ac.tachibana.food_survey.configuration.domain.http.HttpAuthenticationConfig
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.http
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware.authenticationTokenCookieName
import jp.ac.tachibana.food_survey.services.auth.AuthenticationService
import jp.ac.tachibana.food_survey.services.auth.domain.AuthToken

class AuthenticationMiddleware[F[_]: Monad](
  authenticationConfig: HttpAuthenticationConfig,
  authenticationService: AuthenticationService[F])
    extends Http4sDslBinCompat[F]:

  private val authenticate: Kleisli[F, Request[F], Either[AuthenticationMiddleware.AuthenticationError, (AuthToken, User.Id)]] =
    Kleisli { request =>
      request.cookies
        .find(_.name === authenticationTokenCookieName)
        .traverse { cookie =>
          val authToken = AuthToken(cookie.content)
          authenticationService
            .authenticate(authToken)
            .map(_.bimap(_ => AuthenticationMiddleware.AuthenticationError.InvalidCredentials, (authToken, _)))
        }
        .map(_.toRight(AuthenticationMiddleware.AuthenticationError.InvalidCredentials).flatten)
    }

  val middleware: AuthMiddleware[F, (AuthToken, User.Id)] =
    AuthMiddleware(
      authUser = authenticate,
      onFailure = AuthedRoutes.of[AuthenticationMiddleware.AuthenticationError, F] { case _ as error =>
        error match {
          case AuthenticationMiddleware.AuthenticationError.InvalidCredentials =>
            Forbidden()
        }
      }
    )

  // todo: ttl
  def withAuthCookie(
    response: Response[F],
    token: AuthToken): Response[F] =
    response.addCookie(
      ResponseCookie(
        name = authenticationTokenCookieName,
        content = token.value,
        path = Some("/"),
        sameSite = authenticationConfig.secure match {
          case HttpAuthenticationConfig.Mode.Insecure => Some(SameSite.Strict)
          case HttpAuthenticationConfig.Mode.Secure   => Some(SameSite.None)
        },
        httpOnly = true,
        domain = Some(authenticationConfig.domain),
        secure = authenticationConfig.secure match {
          case HttpAuthenticationConfig.Mode.Insecure => false
          case HttpAuthenticationConfig.Mode.Secure   => true
        }
      )
    )

object AuthenticationMiddleware:

  val authenticationTokenCookieName = "JFSBSESSIONID"

  sealed trait AuthenticationError

  object AuthenticationError:
    case object InvalidCredentials extends AuthenticationMiddleware.AuthenticationError
