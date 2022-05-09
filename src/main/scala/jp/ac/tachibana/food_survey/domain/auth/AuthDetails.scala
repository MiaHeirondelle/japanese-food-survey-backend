package jp.ac.tachibana.food_survey.domain.auth

import jp.ac.tachibana.food_survey.domain.user.User

sealed trait AuthDetails:
  def token: AuthToken
  def user: User
  def userDataSubmitted: Boolean

object AuthDetails:

  case class Generic(
    token: AuthToken,
    user: User,
    userDataSubmitted: Boolean)
      extends AuthDetails

  case class Respondent(
    token: AuthToken,
    user: User.Respondent,
    userDataSubmitted: Boolean)
      extends AuthDetails

  case class Admin(
    token: AuthToken,
    user: User.Admin)
      extends AuthDetails:

    override def userDataSubmitted: Boolean = true
