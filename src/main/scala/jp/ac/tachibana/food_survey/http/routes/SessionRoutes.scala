package jp.ac.tachibana.food_survey.http.routes

import cats.effect.Async
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.semigroupk.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDslBinCompat
import org.http4s.server.Router
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.{AuthedRoutes, HttpRoutes, Response}

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.http.HttpService
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware
import jp.ac.tachibana.food_survey.http.model.session.websocket.{InputSessionMessageFormat, OutputSessionMessageFormat}
import jp.ac.tachibana.food_survey.http.model.session.{CreateSessionRequest, SessionFormat}
import jp.ac.tachibana.food_survey.programs.session.{SessionListenerProgram, SessionProgram}
import jp.ac.tachibana.food_survey.services.auth.domain.AuthDetails
import jp.ac.tachibana.food_survey.services.session.model.*

class SessionRoutes[F[_]: Async](
  authenticationMiddleware: AuthenticationMiddleware[F],
  sessionProgram: SessionProgram[F],
  sessionListenerProgram: SessionListenerProgram[F]
)(webSocketBuilder: WebSocketBuilder2[F])
    extends HttpService.Routes[F] with Http4sDslBinCompat[F]:

  private def baseRoutes: AuthedRoutes[AuthDetails, F] =
    AuthedRoutes.of {
      case GET -> Root / "active" as _ =>
        for {
          sessionOpt <- sessionProgram.getActiveSession
          result <- Ok(SessionFormat.fromDomainNotFinishedOpt(sessionOpt))
        } yield result

      // GET method to allow websocket connections
      case GET -> Root / "connect" as user =>
        for {
          connected <- sessionListenerProgram.connect(socketListenerResponseBuilder)(user.user)
          result <- connected match {
            case Right(response) =>
              response.pure[F]

            case Left(_) =>
              Conflict()
          }
        } yield result
    }

  private def respondentOnlyRoutes: AuthedRoutes[AuthDetails.Respondent, F] =
    AuthedRoutes.of { case POST -> Root / "join" as respondent =>
      for {
        sessionJoined <- sessionProgram.join(respondent.user)
        result <- sessionJoined match {
          case Right(session) =>
            Ok(SessionFormat.fromDomainNotFinished(session))

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
              Ok(SessionFormat.fromDomainNotFinished(session))

            case Left(_) =>
              Conflict()
          }
        } yield result

      case POST -> Root / "stop" as admin =>
        sessionListenerProgram.stop >> sessionProgram.stop >> Ok()
    }

  override val routes: HttpRoutes[F] =
    Router[F](
      "session" -> (authenticationMiddleware.globalMiddleware(baseRoutes) <+> authenticationMiddleware.adminOnlyMiddleware(
        adminOnlyRoutes) <+> authenticationMiddleware.respondentOnlyMiddleware(respondentOnlyRoutes)))

  private def socketListenerResponseBuilder(
    listenerInputTransformer: SessionListenerInputTransformer[F],
    listenerOutput: SessionListenerOutput[F]): F[Response[F]] =
    webSocketBuilder.build(
      send = listenerOutput.map(OutputSessionMessageFormat.toWebSocketFrame),
      receive =
        _.map(InputSessionMessageFormat.fromWebSocketFrame).collect { case Some(i) => i }.through(listenerInputTransformer)
    )
