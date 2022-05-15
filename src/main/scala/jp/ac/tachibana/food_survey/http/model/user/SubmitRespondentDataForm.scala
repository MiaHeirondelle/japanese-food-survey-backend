package jp.ac.tachibana.food_survey.http.model.user

import org.http4s.{ParseFailure, QueryParamDecoder}
import cats.syntax.option.*
import cats.syntax.apply.*
import org.http4s.FormDataDecoder
import org.http4s.FormDataDecoder.*
import jp.ac.tachibana.food_survey.domain.user.RespondentData

case class SubmitRespondentDataForm(
  sex: SubmitRespondentDataForm.Sex,
  age: Int)

object SubmitRespondentDataForm:

  implicit val formDecoder: FormDataDecoder[SubmitRespondentDataForm] =
    (
      field[SubmitRespondentDataForm.Sex]("sex"),
      field[Int]("age")
    ).mapN(SubmitRespondentDataForm.apply)

  enum Sex:
    case Male, Female, Other, Unspecified

  object Sex:

    def fromString(str: String): Option[SubmitRespondentDataForm.Sex] =
      str match {
        case "male"        => SubmitRespondentDataForm.Sex.Male.some
        case "female"      => SubmitRespondentDataForm.Sex.Female.some
        case "other"       => SubmitRespondentDataForm.Sex.Other.some
        case "unspecified" => SubmitRespondentDataForm.Sex.Unspecified.some
        case _             => none
      }

    extension (sex: SubmitRespondentDataForm.Sex)
      def toDomain: RespondentData.Sex =
        sex match {
          case SubmitRespondentDataForm.Sex.Male =>
            RespondentData.Sex.Male
          case SubmitRespondentDataForm.Sex.Female =>
            RespondentData.Sex.Female
          case SubmitRespondentDataForm.Sex.Other =>
            RespondentData.Sex.Other
          case SubmitRespondentDataForm.Sex.Unspecified =>
            RespondentData.Sex.Unspecified
        }

    implicit val queryParamDecoder: QueryParamDecoder[SubmitRespondentDataForm.Sex] =
      QueryParamDecoder.stringQueryParamDecoder.emap(v => fromString(v).toRight(ParseFailure("Unknown respondent sex value", v)))
