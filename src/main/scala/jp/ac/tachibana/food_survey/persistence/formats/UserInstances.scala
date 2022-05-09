package jp.ac.tachibana.food_survey.persistence.formats

import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.{Get, Meta, Put, Read}
import io.circe.{Decoder, Encoder}
import jp.ac.tachibana.food_survey.domain.user.{RespondentData, User, UserCredentials}

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

  implicit val respondentDataSexMeta: Meta[RespondentData.Sex] =
    pgEnumStringOpt[RespondentData.Sex](
      "user_sex",
      {
        case "male"   => Some(RespondentData.Sex.Male)
        case "female" => Some(RespondentData.Sex.Female)
        case _        => None
      },
      {
        case RespondentData.Sex.Male   => "male"
        case RespondentData.Sex.Female => "female"
      }
    )

  implicit val respondentDataAgeMeta: Meta[RespondentData.Age] =
    Meta[Int].imap(RespondentData.Age(_))(_.value)

  implicit val userRead: Read[User] =
    Read[(User.Id, String, User.Role)]
      .map { case (userId, name, role) => User(userId, name, role) }

  implicit val adminRead: Read[User.Admin] =
    Read[(User.Id, String)]
      .map { case (userId, name) => User.Admin(userId, name) }

  implicit val respondentRead: Read[User.Respondent] =
    Read[(User.Id, String)]
      .map { case (userId, name) => User.Respondent(userId, name) }

  implicit val respondentDataRead: Read[RespondentData] =
    Read[(User.Id, Option[RespondentData.Sex], Option[RespondentData.Age])]
      .map { case (userId, sex, age) => RespondentData(userId, sex, age) }

object UserInstances extends UserInstances
