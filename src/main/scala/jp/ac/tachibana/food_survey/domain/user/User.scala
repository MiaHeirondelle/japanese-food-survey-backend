package jp.ac.tachibana.food_survey.domain.user

sealed abstract class User(val role: User.Role):
  def id: User.Id
  def name: String

object User:

  enum Role:
    case Respondent, Admin

  def apply(
    id: User.Id,
    name: String,
    role: User.Role): User =
    role match {
      case User.Role.Respondent =>
        User.Respondent(id, name)
      case User.Role.Admin =>
        User.Admin(id, name)
    }

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
