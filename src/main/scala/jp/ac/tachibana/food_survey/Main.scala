package jp.ac.tachibana.food_survey

import cats.effect.{IO, IOApp}
import doobie.Transactor
import jp.ac.tachibana.food_survey.configuration.domain.ApplicationConfig
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.{RespondentData, User}
import jp.ac.tachibana.food_survey.http.HttpService
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware
import jp.ac.tachibana.food_survey.http.routes.{AuthenticationRoutes, SessionRoutes, UserRoutes}
import jp.ac.tachibana.food_survey.persistence.DatabaseTransactor
import jp.ac.tachibana.food_survey.persistence.auth.*
import jp.ac.tachibana.food_survey.persistence.session.*
import jp.ac.tachibana.food_survey.persistence.user.{PostgresRespondentDataRepository, PostgresUserRepository}
import jp.ac.tachibana.food_survey.programs.auth.DefaultAuthenticationProgram
import jp.ac.tachibana.food_survey.programs.session.{DefaultSessionListenerProgram, DefaultSessionProgram}
import jp.ac.tachibana.food_survey.programs.user.DefaultUserProgram
import jp.ac.tachibana.food_survey.services.auth.DefaultAuthenticationService
import jp.ac.tachibana.food_survey.services.session.managers.*
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
        val respondentDataRepository = new PostgresRespondentDataRepository[IO]()
        val sessionRepository = new PostgresSessionRepository[IO]()
        val sessionTemplateRepository = new PostgresSessionTemplateRepository[IO]

        for {
          awaitingUsersSessionManager <- DefaultAwaitingUsersSessionManager.create[IO]
          inProgressSessionManager <- DefaultInProgressSessionManager.create[IO]
          currentSessionStateManager = new DefaultCurrentSessionStateManager[IO](
            sessionRepository,
            awaitingUsersSessionManager,
            inProgressSessionManager)
          authenticationService <- DefaultAuthenticationService
            .create[IO](authTokenRepository, credentialsRepository, userRepository, respondentDataRepository)
          sessionService = new DefaultSessionService[IO](
            sessionTemplateRepository,
            userRepository,
            currentSessionStateManager,
            awaitingUsersSessionManager,
            inProgressSessionManager)
          sessionListenerService <- DefaultSessionListenerService.create[IO](currentSessionStateManager)
          userService = new DefaultUserService[IO](userRepository, respondentDataRepository)

          userProgram = new DefaultUserProgram[IO](authenticationService, userService)
          sessionProgram = new DefaultSessionProgram[IO](sessionService)
          sessionListenerProgram = new DefaultSessionListenerProgram[IO](sessionService, sessionListenerService)
          authenticationProgram = new DefaultAuthenticationProgram[IO](authenticationService)

          authenticationMiddleware =
            new AuthenticationMiddleware[IO](
              appConfig.http.authentication,
              authenticationProgram
            )

          httpService = new HttpService[IO](
            config = appConfig.http,
            authenticationRoutesBuilder = _ => new AuthenticationRoutes[IO](authenticationMiddleware, authenticationProgram),
            new SessionRoutes[IO](authenticationMiddleware, sessionProgram, sessionListenerProgram)(_),
            _ => new UserRoutes[IO](authenticationMiddleware, userProgram)
          )
          start <- httpService.start(runtime.compute)
        } yield start
      }
    } yield result
