package jp.ac.tachibana.food_survey.http.routes

import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.semigroupk.*
import jp.ac.tachibana.food_survey.domain.auth.AuthDetails
import org.http4s.FormDataDecoder.formEntityDecoder
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDslBinCompat
import org.http4s.server.Router
import org.http4s.{AuthedRoutes, HttpRoutes, ResponseCookie, SameSite}
import jp.ac.tachibana.food_survey.domain.user.{User, UserCredentials}
import jp.ac.tachibana.food_survey.http.HttpService
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware.authenticationTokenCookieName
import jp.ac.tachibana.food_survey.http.model.auth.{LoginForm, UserAuthenticatedResponse}
import jp.ac.tachibana.food_survey.programs.auth.AuthenticationProgram

class AuthenticationRoutes[F[_]: Async](
  authenticationMiddleware: AuthenticationMiddleware[F],
  authenticationProgram: AuthenticationProgram[F])
    extends HttpService.Routes[F] with Http4sDslBinCompat[F]:

  private def login = HttpRoutes.of[F] { case request @ POST -> Root / "login" =>
    request.as[LoginForm].flatMap { form =>
      authenticationProgram
        .login(
          UserCredentials.fromRawValues(
            login = form.login,
            password = form.password
          ))
        .flatMap {
          case Left(_) => Forbidden()
          case Right(details) =>
            Ok(UserAuthenticatedResponse.fromDomain(details.user)).map(r =>
              authenticationMiddleware.withAuthCookie(response = r, token = details.token))
        }
    }
  }
  private def tokenRoutes = AuthedRoutes.of[AuthDetails, F] {
    case GET -> Root / "check" as details =>
      Ok(UserAuthenticatedResponse.fromDomain(details.user))

    case POST -> Root / "logout" as details =>
      authenticationProgram.logout(details.token) >> Ok().map(_.removeCookie(authenticationTokenCookieName))
  }

  private val baseRoutes =
    login <+> authenticationMiddleware.globalMiddleware(tokenRoutes)

  override val routes: HttpRoutes[F] =
    Router[F]("auth" -> baseRoutes)
