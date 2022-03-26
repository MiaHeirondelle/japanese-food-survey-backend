package jp.ac.tachibana.food_survey.domain.session

import cats.data.NonEmptyList
import cats.instances.int.*
import cats.{Eq, Order}

import jp.ac.tachibana.food_survey.domain.question.QuestionAnswer
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
        case AwaitingUsers(_, joinedUsers, waitingForUsers, admin) =>
          NonEmptyList.of(admin, joinedUsers*) ::: waitingForUsers
        case CanBegin(_, joinedUsers, admin) =>
          admin :: joinedUsers
        case InProgress(_, joinedUsers, admin, _, _, _) =>
          admin :: joinedUsers
        case Finished(_, joinedUsers, admin, _) =>
          admin :: joinedUsers
      }

  sealed trait NotFinished extends Session
  sealed trait NotBegan extends Session with Session.NotFinished
  sealed trait InProgressOrFinished extends Session

  case class AwaitingUsers(
    number: Session.Number,
    joinedUsers: List[User.Respondent],
    waitingForUsers: NonEmptyList[User.Respondent],
    admin: User.Admin)
      extends Session.NotBegan:
    val status: Session.Status = Session.Status.AwaitingUsers

  case class CanBegin(
    number: Session.Number,
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin)
      extends Session.NotBegan:
    val status: Session.Status = Session.Status.CanBegin

  case class InProgress(
    number: Session.Number,
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin,
    // todo: refactor to an answers container (map [QuestionId, NonEmptyMap[UserId, Answer]?])
    answers: Vector[QuestionAnswer],
    currentElementNumber: SessionElement.Number,
    template: SessionTemplate)
      extends Session.NotFinished with Session.InProgressOrFinished:
    val status: Session.Status = Session.Status.InProgress

  object InProgress:

    def fromTemplate(
      session: Session.CanBegin,
      template: SessionTemplate): Session.InProgress =
      Session.InProgress(
        session.number,
        session.joinedUsers,
        session.admin,
        answers = Vector.empty,
        currentElementNumber = SessionElement.Number.zero,
        template
      )

  case class Finished(
    number: Session.Number,
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin,
    answers: NonEmptyList[QuestionAnswer])
      extends Session.InProgressOrFinished:
    val status: Session.Status = Session.Status.Finished
