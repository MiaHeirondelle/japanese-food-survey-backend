package jp.ac.tachibana.food_survey.domain.session

import cats.data.NonEmptyList
import cats.instances.int.*
import cats.{Eq, Order}

import jp.ac.tachibana.food_survey.domain.user.User

sealed abstract class Session:
  def status: Session.Status
  def number: Session.Number
  def admin: User.Admin

object Session:

  opaque type Number = Int

  object Number:

    val zero: Number = 0

    implicit val order: Order[Number] =
      catsKernelStdOrderForInt

    extension (number: Number)
      def increment: Number = number + 1
      def value: Int = number

    def apply(number: Int): Number = number

  enum Status:
    case AwaitingUsers, CanBegin, InProgress, Finished

  object Status:

    implicit val eq: Eq[Session.Status] =
      Eq.fromUniversalEquals

  extension (session: Session)
    def participants: NonEmptyList[User] =
      session match {
        case AwaitingUsers(number, joinedUsers, waitingForUsers, admin) =>
          NonEmptyList.of(admin, joinedUsers*) ::: waitingForUsers
        case CanBegin(number, joinedUsers, admin) =>
          admin :: joinedUsers
        case InProgress(number, joinedUsers, admin) =>
          admin :: joinedUsers
        case Finished(number, joinedUsers, admin) =>
          admin :: joinedUsers
      }

  sealed trait NotFinished extends Session
  sealed trait NotBegan extends Session

  case class AwaitingUsers(
    number: Session.Number,
    joinedUsers: List[User.Respondent],
    waitingForUsers: NonEmptyList[User.Respondent],
    admin: User.Admin)
      extends NotFinished with NotBegan:
    val status: Session.Status = Session.Status.AwaitingUsers

  case class CanBegin(
    number: Session.Number,
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin)
      extends NotFinished with NotBegan:
    val status: Session.Status = Session.Status.CanBegin

  case class InProgress(
    number: Session.Number,
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin
    // todo: remaining questions (nel)
    // todo: replies
  ) extends NotFinished:
    val status: Session.Status = Session.Status.InProgress

  case class Finished(
    number: Session.Number,
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin
    // todo: replies (nel)
  ) extends Session:
    val status: Session.Status = Session.Status.Finished
