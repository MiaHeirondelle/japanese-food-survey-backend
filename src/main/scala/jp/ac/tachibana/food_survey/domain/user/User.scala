package jp.ac.tachibana.food_survey.domain.user

sealed trait User:
  def id: User.Id
  def name: String

object User:

  case class Respondent(
    id: User.Id,
    name: String)
      extends User

  case class Admin(
    id: User.Id,
    name: String)
      extends User

  opaque type Id = String

  object Id:

    def apply(userId: String): User.Id = userId

  extension (userId: User.Id) def value: String = userId
