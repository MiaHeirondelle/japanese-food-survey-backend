package jp.ac.tachibana.food_survey.http.routes

import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.semigroupk.*
import cats.syntax.option.*
import jp.ac.tachibana.food_survey.domain.auth.AuthDetails
import jp.ac.tachibana.food_survey.domain.user.{RespondentData, User, UserCredentials}
import jp.ac.tachibana.food_survey.http.HttpService
import jp.ac.tachibana.food_survey.http.middleware.AuthenticationMiddleware
import jp.ac.tachibana.food_survey.http.model.session.SessionFormat
import jp.ac.tachibana.food_survey.http.model.user.{CreateUserForm, SubmitRespondentDataForm, UserListResponse}
import jp.ac.tachibana.food_survey.programs.user.UserProgram
import jp.ac.tachibana.food_survey.services.session.SessionService
import org.http4s.FormDataDecoder.formEntityDecoder
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDslBinCompat
import org.http4s.server.Router
import org.http4s.{AuthedRoutes, HttpRoutes}

class UserRoutes[F[_]: Async](
  authenticationMiddleware: AuthenticationMiddleware[F],
  userProgram: UserProgram[F])
    extends HttpService.Routes[F] with Http4sDslBinCompat[F]:

  private def adminRoutes: AuthedRoutes[AuthDetails.Admin, F] =
    AuthedRoutes.of {
      case request @ PUT -> Root / "create" as _ =>
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
      case request @ GET -> Root / "respondent" / "get" / "all" as _ =>
        userProgram
          .getAllByRole(User.Role.Respondent)
          .flatMap(users => Ok(UserListResponse.fromDomain(users)))
    }

  private def respondentOnlyRoutes: AuthedRoutes[AuthDetails.Respondent, F] =
    AuthedRoutes.of { case request @ PUT -> Root / "respondent" / "data" as details =>
      if (details.userDataSubmitted)
        Ok()
      else
        request.req.as[SubmitRespondentDataForm].flatMap { form =>
          userProgram.submitRespondentData(
            RespondentData(
              details.user.id,
              form.sex.toDomain.some,
              RespondentData.Age(form.age).some
            )
          ) >> Created()
        }
    }

  override val routes: HttpRoutes[F] =
    Router[F](
      "user" -> (authenticationMiddleware.adminOnlyMiddleware(adminRoutes) <+> authenticationMiddleware.respondentOnlyMiddleware(
        respondentOnlyRoutes)))
