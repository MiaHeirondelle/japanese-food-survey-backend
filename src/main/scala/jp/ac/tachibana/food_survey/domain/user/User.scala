package jp.ac.tachibana.food_survey.domain.user

case class User(
  id: User.Id,
  name: String,
  role: User.Role)

object User:

  opaque type Id = String

  object Id:

    def apply(userId: String): User.Id = userId

  extension (userId: User.Id) def value: String = userId

  enum Role:
    case Admin
    case Ordinary
