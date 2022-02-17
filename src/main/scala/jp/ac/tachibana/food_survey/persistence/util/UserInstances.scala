package jp.ac.tachibana.food_survey.persistence.util

import io.circe.{Decoder, Encoder}
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.{Get, Meta, Put, Read}

import jp.ac.tachibana.food_survey.domain.user.{User, UserCredentials}

trait UserInstances:

  implicit val userIdEncoder: Encoder[User.Id] =
    Encoder.encodeString.contramap(_.value)

  implicit val userIdDecoder: Decoder[User.Id] =
    Decoder.decodeString.map(User.Id(_))

  implicit val userIdMeta: Meta[User.Id] = Meta[String].imap(User.Id(_))(_.value)

  implicit val userLoginMeta: Meta[UserCredentials.Login] = Meta[String].imap(UserCredentials.Login(_))(_.value)

  implicit val userRoleMeta: Meta[User.Role] =
    pgEnumStringOpt[User.Role](
      "user_role",
      {
        case "respondent" => Some(User.Role.Respondent)
        case "admin"      => Some(User.Role.Admin)
        case _            => None
      },
      {
        case User.Role.Respondent => "respondent"
        case User.Role.Admin      => "admin"
      }
    )

  implicit val userRead: Read[User] =
    Read[(User.Id, String, User.Role)]
      .map { case (userId, name, role) => User(userId, name, role) }

object UserInstances extends UserInstances
