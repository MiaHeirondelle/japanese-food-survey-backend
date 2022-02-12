package jp.ac.tachibana.food_survey.http.model.user

import cats.syntax.apply.*
import org.http4s.FormDataDecoder
import org.http4s.FormDataDecoder.*

import jp.ac.tachibana.food_survey.http.model.auth.LoginForm

case class CreateUserForm(
  name: String,
  login: String,
  password: String,
  role: UserRoleFormat)

object CreateUserForm:

  implicit val formDecoder: FormDataDecoder[CreateUserForm] =
    (
      field[String]("name"),
      field[String]("login"),
      field[String]("password"),
      field[UserRoleFormat]("role")
    ).mapN(CreateUserForm.apply)
