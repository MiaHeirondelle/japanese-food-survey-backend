package jp.ac.tachibana.food_survey.http

import scala.concurrent.ExecutionContext

import cats.effect.Async
import cats.syntax.semigroupk.*
import cats.{Applicative, Monad, Monoid, MonoidK, SemigroupK}
import javax.net.ssl.SSLContext
import org.http4s.blaze.server.*
import org.http4s.headers.Origin
import org.http4s.implicits.*
import org.http4s.server.middleware.{CORS, CORSConfig}
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.{Http, HttpApp, HttpRoutes, Method}

import jp.ac.tachibana.food_survey.configuration.domain.http.{CorsConfig, HttpConfig}
import jp.ac.tachibana.food_survey.http
import jp.ac.tachibana.food_survey.http.routes.AuthenticationRoutes

class HttpService[F[_]: Async](
  config: HttpConfig,
  authenticationRoutesBuilder: WebSocketBuilder2[F] => AuthenticationRoutes[F],
  routeBuilders: (WebSocketBuilder2[F] => HttpService.Routes[F])*):

  private def httpApp(wsb: WebSocketBuilder2[F]): HttpApp[F] =
    withCORS(
      // It's important that routes that do not require authentication go first.
      // Otherwise, the service will short-circuit with a 409 error.
      (authenticationRoutesBuilder(wsb).routes <+> routeBuilders
        .map(b => b(wsb).routes)
        .fold(HttpRoutes.empty[F])(_ <+> _)).orNotFound)

  def start(ec: ExecutionContext): F[Unit] =
    BlazeServerBuilder[F]
      .withExecutionContext(ec)
      .withHttpWebSocketApp(httpApp)
      .bindHttp(port = config.port, host = config.host)
      .serve
      .compile
      .drain

  private def withCORS(service: Http[F, F]): Http[F, F] =
    config.cors match {
      case CorsConfig.CorsEnabled(allowedOrigins) =>
        CORS.policy
          .withAllowOriginHeader(allowedOrigins)
          .withAllowMethodsIn(Set(Method.OPTIONS, Method.HEAD, Method.GET, Method.POST, Method.PUT))
          .withAllowCredentials(true)
          .apply(service)
      case CorsConfig.CorsDisabled =>
        service
    }

object HttpService:

  trait Routes[F[_]]:

    def routes: HttpRoutes[F]
