package jp.ac.tachibana.food_survey.http.model.user

import io.circe.Encoder

import jp.ac.tachibana.food_survey.domain.user.User

case class UserListResponse(users: List[UserFormat]) derives Encoder.AsObject

object UserListResponse:

  def fromDomain(users: List[User]): UserListResponse =
    UserListResponse(users.map(UserFormat.fromDomain))
