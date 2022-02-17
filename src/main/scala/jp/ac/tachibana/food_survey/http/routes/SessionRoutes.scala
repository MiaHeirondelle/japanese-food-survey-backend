package jp.ac.tachibana.food_survey.http.routes

import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDslBinCompat
import org.http4s.server.Router
import org.http4s.{AuthedRoutes, HttpRoutes}

import jp.ac.tachibana.food_survey.http.HttpService
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware
import jp.ac.tachibana.food_survey.http.model.session.SessionResponse
import jp.ac.tachibana.food_survey.services.auth.domain.AuthDetails
import jp.ac.tachibana.food_survey.services.session.SessionService

class SessionRoutes[F[_]: Async](
  authenticationMiddleware: AuthenticationMiddleware[F],
  sessionService: SessionService[F])
    extends HttpService.Routes[F] with Http4sDslBinCompat[F]:

  private val baseRoutes: AuthedRoutes[AuthDetails, F] =
    AuthedRoutes.of { case GET -> Root / "status" as _ =>
      for {
        sessionOpt <- sessionService.getActiveSession
        response = SessionResponse.fromDomain(sessionOpt)
        result <- Ok(response)
      } yield result
    }

  override val routes: HttpRoutes[F] =
    Router[F]("session" -> authenticationMiddleware.globalMiddleware(baseRoutes))
