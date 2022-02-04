package jp.ac.tachibana.food_survey.http.model.auth

import cats.syntax.apply.*
import org.http4s.FormDataDecoder
import org.http4s.FormDataDecoder.*

import jp.ac.tachibana.food_survey.http.model.auth.LoginForm

case class LoginForm(
  login: String,
  password: String)

object LoginForm:
  implicit val formDecoder: FormDataDecoder[LoginForm] =
    (
      field[String]("login"),
      field[String]("password")
    ).mapN(LoginForm.apply)
