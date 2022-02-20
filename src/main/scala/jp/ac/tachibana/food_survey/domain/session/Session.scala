package jp.ac.tachibana.food_survey.domain.session

import cats.data.NonEmptyList

import jp.ac.tachibana.food_survey.domain.user.User

sealed abstract class Session(val status: Session.Status):
  def number: Session.Number
  def admin: User.Admin

object Session:

  opaque type Number = Int

  object Number:

    val zero: Number = 0

    extension (number: Number)
      def increment: Number = number + 1
      def value: Int = number

    def apply(number: Int): Number = number

  case class AwaitingUsers(
    number: Session.Number,
    joinedUsers: List[User.Respondent],
    waitingForUsers: NonEmptyList[User.Respondent],
    admin: User.Admin)
      extends Session(Status.AwaitingUsers)

  case class CanBegin(
    number: Session.Number,
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin)
      extends Session(Status.CanBegin)

  case class InProgress(
    number: Session.Number,
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin
    // todo: remaining questions (nel)
    // todo: replies
  ) extends Session(Status.InProgress)

  case class Finished(
    number: Session.Number,
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin
    // todo: replies (nel)
  ) extends Session(Status.Finished)

  enum Status:
    case AwaitingUsers, CanBegin, InProgress, Finished
