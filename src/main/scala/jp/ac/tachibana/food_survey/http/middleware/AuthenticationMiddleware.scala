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
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware.{
  adminOnlyAuthDetailsTransformer,
  authenticationTokenCookieName,
  defaultAuthDetailsTransformer,
  respondentOnlyAuthDetailsTransformer
}
import jp.ac.tachibana.food_survey.services.auth.{domain, AuthenticationService}
import jp.ac.tachibana.food_survey.services.auth.domain.{AuthDetails, AuthToken}

class AuthenticationMiddleware[F[_]: Monad](
  authenticationConfig: HttpAuthenticationConfig,
  authenticationService: AuthenticationService[F])
    extends Http4sDsl[F]:

  // todo: fast middleware that doesn't load the user?
  val globalMiddleware: AuthMiddleware[F, AuthDetails] =
    AuthMiddleware(
      authUser = authenticate(),
      onFailure = authFailureHandler
    )
  val adminOnlyMiddleware: AuthMiddleware[F, AuthDetails.Admin] =
    AuthMiddleware(
      authUser = authenticate(adminOnlyAuthDetailsTransformer),
      onFailure = authFailureHandler
    )

  val respondentOnlyMiddleware: AuthMiddleware[F, AuthDetails.Respondent] =
    AuthMiddleware(
      authUser = authenticate(respondentOnlyAuthDetailsTransformer),
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

  private def authenticate[A <: AuthDetails](
    authDetailsTransformer: (AuthToken, User) => Option[A] = defaultAuthDetailsTransformer)
    : Kleisli[F, Request[F], Either[AuthenticationMiddleware.AuthenticationError, A]] =
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
                authDetailsTransformer(authToken, user) match {
                  case Some(details) => details.asRight
                  case None          => AuthenticationMiddleware.AuthenticationError.AccessDenied.asLeft
                }
            }
        }
        .map(_.toRight(AuthenticationMiddleware.AuthenticationError.InvalidCredentials).flatten)
    }

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

  private def defaultAuthDetailsTransformer(
    authToken: AuthToken,
    user: User): Option[AuthDetails] =
    Some(AuthDetails.Generic(authToken, user))

  private def respondentOnlyAuthDetailsTransformer(
    authToken: AuthToken,
    user: User): Option[AuthDetails.Respondent] =
    user match {
      case respondent: User.Respondent =>
        Some(AuthDetails.Respondent(authToken, respondent))
      case _: User.Admin =>
        None
    }

  private def adminOnlyAuthDetailsTransformer(
    authToken: AuthToken,
    user: User): Option[AuthDetails.Admin] =
    user match {
      case admin: User.Admin =>
        Some(AuthDetails.Admin(authToken, admin))
      case _: User.Respondent =>
        None
    }

  sealed trait AuthenticationError

  object AuthenticationError:
    case object InvalidCredentials extends AuthenticationMiddleware.AuthenticationError
    case object AccessDenied extends AuthenticationMiddleware.AuthenticationError
