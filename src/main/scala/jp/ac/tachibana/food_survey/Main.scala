package jp.ac.tachibana.food_survey

import cats.effect.{IO, IOApp}
import doobie.Transactor

import jp.ac.tachibana.food_survey.configuration.domain.ApplicationConfig
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.http.HttpService
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware
import jp.ac.tachibana.food_survey.http.routes.{AuthenticationRoutes, SessionRoutes, UserRoutes}
import jp.ac.tachibana.food_survey.persistence.DatabaseTransactor
import jp.ac.tachibana.food_survey.persistence.auth.{PostgresAuthTokenRepository, PostgresCredentialsRepository}
import jp.ac.tachibana.food_survey.persistence.session.{CachingPostgresSessionRepository, PostgresSessionRepository}
import jp.ac.tachibana.food_survey.persistence.user.PostgresUserRepository
import jp.ac.tachibana.food_survey.programs.session.{DefaultSessionListenerProgram, DefaultSessionProgram}
import jp.ac.tachibana.food_survey.programs.user.DefaultUserProgram
import jp.ac.tachibana.food_survey.services.auth.DefaultAuthenticationService
import jp.ac.tachibana.food_survey.services.session.{DefaultSessionListenerService, DefaultSessionService}
import jp.ac.tachibana.food_survey.services.user.DefaultUserService

object Main extends IOApp.Simple:

  override def run: IO[Unit] =
    for {
      appConfig <- ApplicationConfig.load[IO]
      _ <- IO.delay(println(appConfig))
      result <- DatabaseTransactor.start[IO](appConfig.persistence).use { (tr: Transactor[IO]) =>
        implicit val transactor: Transactor[IO] = tr
        val authTokenRepository = new PostgresAuthTokenRepository[IO]()
        val credentialsRepository = new PostgresCredentialsRepository[IO]()
        val userRepository = new PostgresUserRepository[IO]()

        for {
          sessionRepository <- CachingPostgresSessionRepository.make[IO]

          authenticationService <- DefaultAuthenticationService
            .create[IO](authTokenRepository, credentialsRepository, userRepository)
          sessionService = new DefaultSessionService[IO](sessionRepository, userRepository)
          sessionListenerService <- DefaultSessionListenerService.create[IO](sessionRepository)
          authenticationMiddleware =
            new AuthenticationMiddleware[IO](
              appConfig.http.authentication,
              authenticationService
            )
          userService = new DefaultUserService[IO](userRepository)

          userProgram = new DefaultUserProgram[IO](authenticationService, userService)
          sessionProgram = new DefaultSessionProgram[IO](sessionService)
          sessionListenerProgram = new DefaultSessionListenerProgram(sessionService, sessionListenerService)

          httpService = new HttpService[IO](
            config = appConfig.http,
            authenticationRoutesBuilder = _ => new AuthenticationRoutes[IO](authenticationMiddleware, authenticationService),
            new SessionRoutes[IO](authenticationMiddleware, sessionProgram, sessionListenerProgram)(_),
            _ => new UserRoutes[IO](authenticationMiddleware, userProgram)
          )
          start <- httpService.start(runtime.compute)
        } yield start
      }
    } yield result
