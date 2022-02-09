package jp.ac.tachibana.food_survey.http.model.auth

import io.circe.Encoder

import jp.ac.tachibana.food_survey.domain.user.User

case class UserAuthenticatedResponse(
  id: String,
  role: UserRoleFormat)
    derives Encoder.AsObject

object UserAuthenticatedResponse:

  def fromDomain(user: User): UserAuthenticatedResponse =
    UserAuthenticatedResponse(
      id = user.id.value,
      role = UserRoleFormat.fromDomain(user)
    )
