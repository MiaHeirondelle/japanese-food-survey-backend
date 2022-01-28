package jp.ac.tachibana.food_survey.http

import scala.concurrent.ExecutionContext

import cats.effect.Async
import cats.syntax.semigroupk.*
import cats.{Applicative, Monad, Monoid, MonoidK, SemigroupK}
import javax.net.ssl.SSLContext
import org.http4s.blaze.server.*
import org.http4s.implicits.*
import org.http4s.{HttpApp, HttpRoutes}

import jp.ac.tachibana.food_survey.configuration.domain.HttpConfig
import jp.ac.tachibana.food_survey.http
import jp.ac.tachibana.food_survey.http.routes.AuthenticationRoutes

class HttpService[F[_]: Async](
  config: HttpConfig,
  sslContext: Option[SSLContext],
  authenticationRoutes: AuthenticationRoutes[F],
  routes: HttpService.Routes[F]*):

  private val httpApp: HttpApp[F] =
    (authenticationRoutes.routes <+> routes
      .map(_.routes)
      .fold(HttpRoutes.empty[F])(_ <+> _)).orNotFound

  def start(ec: ExecutionContext): F[Unit] = {

    val baseServer = BlazeServerBuilder[F]
      .withExecutionContext(ec)
      .withHttpApp(httpApp)

    sslContext
      .fold(baseServer)(baseServer.withSslContext)
      .bindHttp(port = config.port, host = config.host)
      .serve
      .compile
      .drain
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
