package jp.ac.tachibana.food_survey.http.middleware

import cats.Monad
import cats.data.{Kleisli, OptionT}
import cats.instances.either.*
import cats.instances.option.*
import cats.syntax.either.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, Request, Response, ResponseCookie, SameSite}

import jp.ac.tachibana.food_survey.configuration.domain.http.HttpAuthenticationConfig
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.http
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware.{adminOnlyFilter, authenticationTokenCookieName}
import jp.ac.tachibana.food_survey.services.auth.AuthenticationService
import jp.ac.tachibana.food_survey.services.auth.domain.{AuthDetails, AuthToken}

class AuthenticationMiddleware[F[_]: Monad](
  authenticationConfig: HttpAuthenticationConfig,
  authenticationService: AuthenticationService[F])
    extends Http4sDsl[F]:

  private def authenticate(userFilter: User => Boolean = _ => true)
    : Kleisli[F, Request[F], Either[AuthenticationMiddleware.AuthenticationError, AuthDetails]] =
    Kleisli { request =>
      request.cookies
        .find(_.name === authenticationTokenCookieName)
        .traverse { cookie =>
          val authToken = AuthToken(cookie.content)
          authenticationService
            .authenticate(authToken)
            .map {
              case Left(_) =>
                AuthenticationMiddleware.AuthenticationError.InvalidCredentials.asLeft
              case Right(user) =>
                println(user)
                if (userFilter(user))
                  AuthDetails(authToken, user).asRight
                else
                  AuthenticationMiddleware.AuthenticationError.AccessDenied.asLeft
            }
        }
        .map(_.toRight(AuthenticationMiddleware.AuthenticationError.InvalidCredentials).flatten)
    }

  // todo: fast middleware that doesn't load the user?
  val globalMiddleware: AuthMiddleware[F, AuthDetails] =
    AuthMiddleware(
      authUser = authenticate(),
      onFailure = authFailureHandler
    )

  // todo: transform auth details to admin user?
  val adminOnlyMiddleware: AuthMiddleware[F, AuthDetails] =
    AuthMiddleware(
      authUser = authenticate(adminOnlyFilter),
      onFailure = authFailureHandler
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

  private def authFailureHandler: AuthedRoutes[AuthenticationMiddleware.AuthenticationError, F] =
    AuthedRoutes.of { case _ as error =>
      error match {
        case AuthenticationMiddleware.AuthenticationError.InvalidCredentials =>
          Forbidden()
        case AuthenticationMiddleware.AuthenticationError.AccessDenied =>
          // todo: unauthorized?
          Forbidden()
      }
    }

object AuthenticationMiddleware:

  val authenticationTokenCookieName = "JFSBSESSIONID"

  sealed trait AuthenticationError

  object AuthenticationError:
    case object InvalidCredentials extends AuthenticationMiddleware.AuthenticationError
    case object AccessDenied extends AuthenticationMiddleware.AuthenticationError

  private def adminOnlyFilter(user: User): Boolean =
    user match {
      case _: User.Respondent => false
      case _: User.Admin      => true
    }
