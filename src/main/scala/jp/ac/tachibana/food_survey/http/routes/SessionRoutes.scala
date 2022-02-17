package jp.ac.tachibana.food_survey.http.routes

import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.semigroupk.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDslBinCompat
import org.http4s.server.Router
import org.http4s.{AuthedRoutes, HttpRoutes}

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.http.HttpService
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware
import jp.ac.tachibana.food_survey.http.model.session.{CreateSessionRequest, SessionResponse}
import jp.ac.tachibana.food_survey.programs.session.SessionProgram
import jp.ac.tachibana.food_survey.services.auth.domain.AuthDetails

class SessionRoutes[F[_]: Async](
  authenticationMiddleware: AuthenticationMiddleware[F],
  sessionProgram: SessionProgram[F])
    extends HttpService.Routes[F] with Http4sDslBinCompat[F]:

  private def baseRoutes: AuthedRoutes[AuthDetails, F] =
    AuthedRoutes.of { case GET -> Root / "status" as _ =>
      for {
        sessionOpt <- sessionProgram.getActiveSession
        result <- Ok(SessionResponse.fromDomain(sessionOpt))
      } yield result
    }

  private def adminOnlyRoutes: AuthedRoutes[AuthDetails.Admin, F] =
    AuthedRoutes.of { case request @ POST -> Root / "create" as admin =>
      for {
        createSessionRequest <- request.req.as[CreateSessionRequest]
        sessionCreated <- sessionProgram.create(admin.user, createSessionRequest.respondents.map(User.Id(_)))
        result <- sessionCreated match {
          case Right(session) =>
            Ok(SessionResponse.fromDomain(Some(session)))

          case Left(_) =>
            Conflict()
        }
      } yield result
    }

  override val routes: HttpRoutes[F] =
    Router[F](
      "session" -> (authenticationMiddleware.globalMiddleware(baseRoutes) <+> authenticationMiddleware.adminOnlyMiddleware(
        adminOnlyRoutes)))
