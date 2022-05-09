package jp.ac.tachibana.food_survey.domain.user

case class RespondentData(
  userId: User.Id,
  sex: Option[RespondentData.Sex],
  age: Option[RespondentData.Age])

object RespondentData:

  enum Sex:
    case Male, Female

  opaque type Age = Int

  object Age:

    extension (age: Age) def value: Int = age

    def apply(age: Int): RespondentData.Age =
      age
