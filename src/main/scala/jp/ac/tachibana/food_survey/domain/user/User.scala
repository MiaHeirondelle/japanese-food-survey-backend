package jp.ac.tachibana.food_survey.domain.user

sealed abstract class User(val role: User.Role):
  def id: User.Id
  def name: String

object User:

  enum Role:
    case Respondent, Admin

  case class Respondent(
    id: User.Id,
    name: String)
      extends User(Role.Respondent)

  case class Admin(
    id: User.Id,
    name: String)
      extends User(Role.Admin)

  opaque type Id = String

  object Id:

    def apply(userId: String): User.Id = userId

    extension (userId: User.Id) def value: String = userId
