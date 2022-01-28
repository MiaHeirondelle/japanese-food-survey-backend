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

import jp.ac.tachibana.food_survey.configuration.domain.authentication.{AuthenticationConfig, SSLConfig}
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.http
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware.authenticationTokenCookieName
import jp.ac.tachibana.food_survey.services.authentication.AuthenticationService
import jp.ac.tachibana.food_survey.services.authentication.domain.AuthToken

class AuthenticationMiddleware[F[_]: Monad](
  authenticationConfig: AuthenticationConfig,
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

  val accessMiddleware: AuthMiddleware[F, (AuthToken, User.Id)] =
    AuthMiddleware(
      authUser = authenticate,
      onFailure = AuthedRoutes.of[AuthenticationMiddleware.AuthenticationError, F] { case _ as error =>
        error match {
          case AuthenticationMiddleware.AuthenticationError.InvalidCredentials =>
            Forbidden()
        }
      }
    )

  def withAuthCookie(
    response: Response[F],
    token: AuthToken): Response[F] =
    response.addCookie(
      ResponseCookie(
        name = authenticationTokenCookieName,
        content = token.value,
        path = Some("/"),
        sameSite = Some(SameSite.Strict),
        httpOnly = true,
        domain = Some(authenticationConfig.domain),
        secure = authenticationConfig.ssl match {
          case SSLConfig.SSLDisabled   => false
          case _: SSLConfig.SSLEnabled => true
        }
      )
    )

object AuthenticationMiddleware:

  val authenticationTokenCookieName = "JFSBSESSIONID"

  sealed trait LoginError

  object LoginError:
    case object InvalidCredentials extends AuthenticationMiddleware.LoginError

  sealed trait AuthenticationError

  object AuthenticationError:
    case object InvalidCredentials extends AuthenticationMiddleware.AuthenticationError
