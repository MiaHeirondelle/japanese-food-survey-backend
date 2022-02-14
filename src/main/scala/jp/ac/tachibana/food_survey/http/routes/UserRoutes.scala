package jp.ac.tachibana.food_survey.http.routes

import cats.effect.Async
import cats.syntax.flatMap.*
import org.http4s.FormDataDecoder.formEntityDecoder
import org.http4s.dsl.Http4sDslBinCompat
import org.http4s.server.Router
import org.http4s.{AuthedRoutes, HttpRoutes}

import jp.ac.tachibana.food_survey.domain.user.UserCredentials
import jp.ac.tachibana.food_survey.http.HttpService
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware
import jp.ac.tachibana.food_survey.http.model.session.SessionResponse
import jp.ac.tachibana.food_survey.http.model.user.CreateUserForm
import jp.ac.tachibana.food_survey.programs.user.UserProgram
import jp.ac.tachibana.food_survey.services.auth.domain.AuthDetails
import jp.ac.tachibana.food_survey.services.session.SessionService

class UserRoutes[F[_]: Async](
  authenticationMiddleware: AuthenticationMiddleware[F],
  userProgram: UserProgram[F])
    extends HttpService.Routes[F] with Http4sDslBinCompat[F]:

  private val adminRoutes: AuthedRoutes[AuthDetails, F] =
    AuthedRoutes.of { case request @ PUT -> Root / "create" as _ =>
      request.req.as[CreateUserForm].flatMap { form =>
        userProgram.create(
          name = form.name,
          role = form.role.domain,
          credentials = UserCredentials.fromRawValues(
            login = form.login,
            password = form.password
          )
        ) >> Created()
      }
    }

  override val routes: HttpRoutes[F] =
    Router[F]("user" -> authenticationMiddleware.adminOnlyMiddleware(adminRoutes))
