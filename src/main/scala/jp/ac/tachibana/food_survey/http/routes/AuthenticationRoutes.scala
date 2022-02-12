package jp.ac.tachibana.food_survey.http.routes

import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.semigroupk.*
import org.http4s.FormDataDecoder.formEntityDecoder
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDslBinCompat
import org.http4s.server.Router
import org.http4s.{AuthedRoutes, HttpRoutes, ResponseCookie, SameSite}

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.http.HttpService
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware.authenticationTokenCookieName
import jp.ac.tachibana.food_survey.http.model.auth.{LoginForm, UserAuthenticatedResponse}
import jp.ac.tachibana.food_survey.services.auth.AuthenticationService
import jp.ac.tachibana.food_survey.services.auth.domain.AuthDetails

class AuthenticationRoutes[F[_]: Async](
  authenticationMiddleware: AuthenticationMiddleware[F],
  authenticationService: AuthenticationService[F])
    extends HttpService.Routes[F] with Http4sDslBinCompat[F]:

  private val login = HttpRoutes.of[F] { case request @ POST -> Root / "login" =>
    request.as[LoginForm].flatMap { form =>
      authenticationService
        .login(form.login, form.password)
        .flatMap {
          case Left(_) => Forbidden()
          case Right(details) =>
            Ok(UserAuthenticatedResponse.fromDomain(details.user)).map(r =>
              authenticationMiddleware.withAuthCookie(response = r, token = details.token))
        }
    }
  }

  private val tokenRoutes = AuthedRoutes.of[AuthDetails, F] {
    case GET -> Root / "check" as details =>
      println(details)
      Ok(UserAuthenticatedResponse.fromDomain(details.user))

    case POST -> Root / "logout" as details =>
      authenticationService.logout(details.token) >> Ok().map(_.removeCookie(authenticationTokenCookieName))
  }

  private val baseRoutes =
    login <+> authenticationMiddleware.globalMiddleware(tokenRoutes)

  override val routes: HttpRoutes[F] =
    Router[F]("auth" -> baseRoutes)
