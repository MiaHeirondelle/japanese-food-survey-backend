package jp.ac.tachibana.food_survey.domain.user

case class UserData(
  userId: User.Id,
  sex: Option[UserData.Sex],
  age: Option[UserData.Age])

object UserData:

  enum Sex:
    case Male, Female

  opaque type Age = Int

  object Age:

    extension (age: Age) def value: Int = age

    def apply(age: Int): UserData.Age =
      age
