package jp.ac.tachibana.food_survey.http.model.auth

import io.circe.Encoder

import jp.ac.tachibana.food_survey.domain.user.User

enum UserRoleFormat:
  case Respondent, Admin

object UserRoleFormat:

  implicit val encoder: Encoder[UserRoleFormat] =
    Encoder.encodeString.contramap {
      case UserRoleFormat.Respondent =>
        "respondent"
      case UserRoleFormat.Admin =>
        "admin"
    }

  def fromDomain(user: User): UserRoleFormat =
    user match {
      case _: User.Admin =>
        UserRoleFormat.Admin
      case _: User.Respondent =>
        UserRoleFormat.Respondent
    }
