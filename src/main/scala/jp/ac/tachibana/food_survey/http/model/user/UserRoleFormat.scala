package jp.ac.tachibana.food_survey.http.model.user

import io.circe.Encoder

import jp.ac.tachibana.food_survey.domain.user.User
import org.http4s.{ParseFailure, QueryParamDecoder}

import jp.ac.tachibana.food_survey.domain.user.User.Role

enum UserRoleFormat:
  case Respondent, Admin

object UserRoleFormat:

  implicit val queryParamDecoder: QueryParamDecoder[UserRoleFormat] =
    QueryParamDecoder.stringQueryParamDecoder.emap(v => fromString(v).toRight(ParseFailure("Unknown user role value", v)))

  implicit val encoder: Encoder[UserRoleFormat] =
    Encoder.encodeString.contramap {
      case UserRoleFormat.Respondent =>
        "respondent"
      case UserRoleFormat.Admin =>
        "admin"
    }

  def fromDomain(user: User): UserRoleFormat =
    user.role match {
      case Role.Respondent =>
        UserRoleFormat.Respondent
      case Role.Admin =>
        UserRoleFormat.Admin
    }

  def fromString(value: String): Option[UserRoleFormat] =
    value match {
      case "respondent" => Some(UserRoleFormat.Respondent)
      case "admin"      => Some(UserRoleFormat.Admin)
      case _            => None
    }
