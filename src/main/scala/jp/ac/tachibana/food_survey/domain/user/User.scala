package jp.ac.tachibana.food_survey.domain.user

import cats.Order

sealed abstract class User(val role: User.Role):
  def id: User.Id
  def name: String

object User:

  opaque type Id = String

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

  object Id:

    // todo: fix?
    implicit val order: Order[User.Id] =
      Order.from((v1, v2) => v1.value.compare(v2.value))

    def apply(userId: String): User.Id = userId

    extension (userId: User.Id) def value: String = userId

  enum Role:
    case Respondent, Admin
