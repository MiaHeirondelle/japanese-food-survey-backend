package jp.ac.tachibana.food_survey.http.model.auth

import io.circe.Encoder
import jp.ac.tachibana.food_survey.domain.auth.AuthDetails
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.http.model.user.UserRoleFormat

case class UserAuthenticatedResponse(
  id: String,
  name: String,
  role: UserRoleFormat,
  user_data_submitted: Boolean)
    derives Encoder.AsObject

object UserAuthenticatedResponse:

  def fromDomain(authDetails: AuthDetails): UserAuthenticatedResponse =
    UserAuthenticatedResponse(
      id = authDetails.user.id.value,
      name = authDetails.user.name,
      role = UserRoleFormat.fromDomain(authDetails.user),
      user_data_submitted = authDetails.userDataSubmitted
    )
