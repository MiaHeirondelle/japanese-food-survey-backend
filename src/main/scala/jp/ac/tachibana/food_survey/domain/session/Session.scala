package jp.ac.tachibana.food_survey.domain.session

import cats.data.NonEmptyList

import jp.ac.tachibana.food_survey.domain.user.User

sealed trait Session

object Session:

  case class AwaitingUsers(
    joinedUsers: List[User.Respondent],
    waitingForUsers: NonEmptyList[User.Respondent],
    admin: User.Admin)
      extends Session

  case class CanBegin(
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin)
      extends Session

  case class InProgress(
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin
    // todo: remaining questions (nel)
    // todo: replies
  ) extends Session

  case class Finished(
    joinedUsers: NonEmptyList[User.Respondent],
    admin: User.Admin
    // todo: replies (nel)
  ) extends Session
