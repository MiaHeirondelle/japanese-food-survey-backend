package jp.ac.tachibana.food_survey.domain.session

import cats.data.NonEmptyList

import jp.ac.tachibana.food_survey.domain.user.User

sealed abstract class Session(val status: Session.Status):
  def admin: User.Admin

object Session:

  // todo: add session number to session
  opaque type Number = Int

  object Number:

    extension (number: Number) def value: Int = number

    def apply(number: Int): Number = number

  case class AwaitingUsers(
    joinedUsers: List[User.Respondent],
    waitingForUsers: NonEmptyList[User.Respondent],
    admin: User.Admin)
      extends Session(Status.AwaitingUsers)

  case class CanBegin(
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin)
      extends Session(Status.CanBegin)

  case class InProgress(
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin
    // todo: remaining questions (nel)
    // todo: replies
  ) extends Session(Status.InProgress)

  case class Finished(
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin
    // todo: replies (nel)
  ) extends Session(Status.Finished)

  enum Status:
    case AwaitingUsers, CanBegin, InProgress, Finished
