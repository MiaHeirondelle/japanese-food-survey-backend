package jp.ac.tachibana.food_survey.services.session.model

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User

sealed trait OutputSessionMessage

object OutputSessionMessage:
  case class UserJoined(
    user: User,
    session: Session.NotFinished)
      extends OutputSessionMessage

  case class SessionBegan(session: Session.InProgress) extends OutputSessionMessage

  case object Shutdown extends OutputSessionMessage
