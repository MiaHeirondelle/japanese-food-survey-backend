package jp.ac.tachibana.food_survey.http

import scala.concurrent.ExecutionContext

import cats.effect.Async
import cats.syntax.semigroupk.*
import cats.{Applicative, Monad, Monoid, MonoidK, SemigroupK}
import javax.net.ssl.SSLContext
import org.http4s.blaze.server.*
import org.http4s.implicits.*
import org.http4s.{Http, HttpApp, HttpRoutes}
import org.http4s.server.middleware.{CORS, CORSConfig}
import org.http4s.headers.Origin
import org.http4s.Method

import jp.ac.tachibana.food_survey.configuration.domain.http.{CorsConfig, HttpConfig}
import jp.ac.tachibana.food_survey.http
import jp.ac.tachibana.food_survey.http.routes.AuthenticationRoutes

class HttpService[F[_]: Async](
  config: HttpConfig,
  authenticationRoutes: AuthenticationRoutes[F],
  routes: HttpService.Routes[F]*):

  private val httpApp: HttpApp[F] =
    withCORS(
      // It's important that routes that do not require authentication go first.
      // Otherwise, the service will short-circuit with a 409 error.
      (authenticationRoutes.routes <+> routes
        .map(_.routes)
        .fold(HttpRoutes.empty[F])(_ <+> _)).orNotFound)

  def start(ec: ExecutionContext): F[Unit] =
    BlazeServerBuilder[F]
      .withExecutionContext(ec)
      .withHttpApp(httpApp)
      .bindHttp(port = config.port, host = config.host)
      .serve
      .compile
      .drain

  private def withCORS(service: Http[F, F]): Http[F, F] =
    config.cors match {
      case CorsConfig.CorsEnabled(allowedOrigins) =>
        CORS.policy
          .withAllowOriginHeader(allowedOrigins)
          .withAllowMethodsIn(Set(Method.OPTIONS, Method.HEAD, Method.GET))
          .withAllowCredentials(true)
          .apply(service)
      case CorsConfig.CorsDisabled =>
        service
    }

object HttpService:

  trait Routes[F[_]]:

    def routes: HttpRoutes[F]

  object Routes:

    def lift[F[_]: Monad](httpRoutes: HttpRoutes[F]*): HttpService.Routes[F] =
      new HttpService.Routes {
        override val routes: HttpRoutes[F] =
          httpRoutes.fold(HttpRoutes.empty[F])(_ <+> _)
      }
