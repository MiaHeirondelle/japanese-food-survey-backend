package jp.ac.tachibana.food_survey.http.routes

import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.semigroupk.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDslBinCompat
import org.http4s.server.Router
import org.http4s.websocket.WebSocketFrame
import org.http4s.{AuthedRoutes, HttpRoutes}

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.http.HttpService
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware
import jp.ac.tachibana.food_survey.http.model.session.{CreateSessionRequest, SessionFormat}
import jp.ac.tachibana.food_survey.programs.session.{SessionListenerProgram, SessionProgram}
import jp.ac.tachibana.food_survey.services.auth.domain.AuthDetails

class SessionRoutes[F[_]: Async](
  authenticationMiddleware: AuthenticationMiddleware[F],
  sessionProgram: SessionProgram[F])
    extends HttpService.Routes[F] with Http4sDslBinCompat[F]:

  private def baseRoutes: AuthedRoutes[AuthDetails, F] =
    AuthedRoutes.of { case GET -> Root as _ =>
      for {
        sessionOpt <- sessionProgram.getActiveSession
        result <- Ok(SessionFormat.fromDomain(sessionOpt))
      } yield result
    }

  private def respondentOnlyRoutes: AuthedRoutes[AuthDetails.Respondent, F] =
    AuthedRoutes.of { case POST -> Root / "join" as respondent =>
      for {
        sessionJoined <- sessionProgram.join(respondent.user)
        result <- sessionJoined match {
          case Right(session) =>
            Ok()

          case Left(_) =>
            Conflict()
        }
      } yield result
    }

  private def adminOnlyRoutes: AuthedRoutes[AuthDetails.Admin, F] =
    AuthedRoutes.of {
      case request @ POST -> Root / "create" as admin =>
        for {
          createSessionRequest <- request.req.as[CreateSessionRequest]
          sessionCreated <- sessionProgram.create(admin.user, createSessionRequest.respondents.map(User.Id(_)))
          result <- sessionCreated match {
            case Right(session) =>
              Ok(SessionFormat.fromDomain(Some(session)))

            case Left(_) =>
              Conflict()
          }
        } yield result
      case POST -> Root / "begin" as admin =>
        for {
          sessionBegan <- sessionProgram.begin(admin.user)
          result <- sessionBegan match {
            case Right(_) =>
              Ok()

            case Left(_) =>
              Conflict()
          }
        } yield result
      case POST -> Root / "stop" as admin =>
        sessionProgram.stop >> Ok()
    }

  override val routes: HttpRoutes[F] =
    Router[F](
      "session" -> (authenticationMiddleware.globalMiddleware(baseRoutes) <+> authenticationMiddleware.adminOnlyMiddleware(
        adminOnlyRoutes) <+> authenticationMiddleware.respondentOnlyMiddleware(respondentOnlyRoutes)))

object SessionRoutes:

  def convertInputSessionSocketMessage(frame: WebSocketFrame): Option[SessionListenerProgram.InputMessage] =
    frame match {
      case WebSocketFrame.Text(text, _) => ???

    }
