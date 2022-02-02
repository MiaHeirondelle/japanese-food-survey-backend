package jp.ac.tachibana.food_survey.http.routes

import cats.effect.Async
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import cats.syntax.semigroupk.*
import org.http4s.dsl.Http4sDslBinCompat
import org.http4s.server.Router
import org.http4s.{AuthedRoutes, HttpRoutes, ResponseCookie, SameSite}

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.http.HttpService
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware.authenticationTokenCookieName
import jp.ac.tachibana.food_survey.http.model.LoginForm
import org.http4s.FormDataDecoder.formEntityDecoder

import jp.ac.tachibana.food_survey.services.authentication.AuthenticationService
import jp.ac.tachibana.food_survey.services.authentication.domain.AuthToken

class AuthenticationRoutes[F[_]: Async](
  authenticationMiddleware: AuthenticationMiddleware[F],
  authenticationService: AuthenticationService[F])
    extends HttpService.Routes[F] with Http4sDslBinCompat[F]:

  private val login = HttpRoutes.of[F] { case request @ POST -> Root / "login" =>
    request.as[LoginForm].flatMap { form =>
      authenticationService
        .login(form.login, form.password)
        .flatMap {
          case Left(_)      => Forbidden()
          case Right(token) => Ok().map(r => authenticationMiddleware.withAuthCookie(response = r, token = token))
        }
    }
  }

  private val test = AuthedRoutes.of[(AuthToken, User.Id), F] { case POST -> Root / "test" as (token, userId) =>
    Ok(userId.value)
  }

  private val logout = AuthedRoutes.of[(AuthToken, User.Id), F] { case POST -> Root / "logout" as (token, _) =>
    authenticationService.logout(token) >> Ok().map(_.removeCookie(authenticationTokenCookieName))
  }

  private val baseRoutes =
    login <+> authenticationMiddleware.accessMiddleware(test <+> logout)

  override def routes: HttpRoutes[F] =
    Router[F]("auth" -> baseRoutes)
