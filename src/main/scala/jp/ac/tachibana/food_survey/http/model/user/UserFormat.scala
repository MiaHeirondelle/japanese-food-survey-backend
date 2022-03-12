package jp.ac.tachibana.food_survey.http.model.user

import io.circe.Encoder

import jp.ac.tachibana.food_survey.domain.user.User

case class UserFormat(
  id: String,
  name: String,
  role: UserRoleFormat)
    derives Encoder.AsObject

object UserFormat:

  def fromDomain(user: User): UserFormat =
    UserFormat(
      id = user.id.value,
      name = user.name,
      role = UserRoleFormat.fromDomain(user)
    )
