package jp.ac.tachibana.food_survey.persistence.formats

import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.{Get, Meta, Put, Read}
import io.circe.{Decoder, Encoder}
import jp.ac.tachibana.food_survey.domain.user.{User, UserCredentials, UserData}

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

  implicit val userDataSexMeta: Meta[UserData.Sex] =
    pgEnumStringOpt[UserData.Sex](
      "user_sex",
      {
        case "male"   => Some(UserData.Sex.Male)
        case "female" => Some(UserData.Sex.Female)
        case _        => None
      },
      {
        case UserData.Sex.Male   => "male"
        case UserData.Sex.Female => "female"
      }
    )

  implicit val userDataAgeMeta: Meta[UserData.Age] =
    Meta[Int].imap(UserData.Age(_))(_.value)

  implicit val userRead: Read[User] =
    Read[(User.Id, String, User.Role)]
      .map { case (userId, name, role) => User(userId, name, role) }

  implicit val adminRead: Read[User.Admin] =
    Read[(User.Id, String)]
      .map { case (userId, name) => User.Admin(userId, name) }

  implicit val respondentRead: Read[User.Respondent] =
    Read[(User.Id, String)]
      .map { case (userId, name) => User.Respondent(userId, name) }

  implicit val userDataRead: Read[UserData] =
    Read[(User.Id, Option[UserData.Sex], Option[UserData.Age])]
      .map { case (userId, sex, age) => UserData(userId, sex, age) }

object UserInstances extends UserInstances
