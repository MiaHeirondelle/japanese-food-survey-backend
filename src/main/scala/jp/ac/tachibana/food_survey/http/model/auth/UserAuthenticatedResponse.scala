package jp.ac.tachibana.food_survey.http.model.auth

import io.circe.Encoder

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.http.model.user.UserRoleFormat

case class UserAuthenticatedResponse(
  id: String,
  name: String,
  role: UserRoleFormat)
    derives Encoder.AsObject

object UserAuthenticatedResponse:

  def fromDomain(user: User): UserAuthenticatedResponse =
    UserAuthenticatedResponse(
      id = user.id.value,
      name = user.name,
      role = UserRoleFormat.fromDomain(user)
    )
