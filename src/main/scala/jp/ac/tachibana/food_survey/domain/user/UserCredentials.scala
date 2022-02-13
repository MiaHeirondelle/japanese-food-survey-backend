package jp.ac.tachibana.food_survey.domain.user

import sun.security.util.Password

import jp.ac.tachibana.food_survey.util.crypto.Secret

case class UserCredentials(
  login: Secret[UserCredentials.Login],
  password: Secret[UserCredentials.Password])

object UserCredentials:

  opaque type Login = String

  object Login:
    extension (login: Login) def value: String = login

  opaque type Password = String

  object Password:
    extension (password: Password) def value: String = password

  def fromRawValues(
    login: String,
    password: String): UserCredentials =
    UserCredentials(
      Secret(login),
      Secret(password)
    )
