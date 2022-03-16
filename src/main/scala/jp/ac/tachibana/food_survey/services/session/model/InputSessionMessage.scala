package jp.ac.tachibana.food_survey.services.session.model

sealed trait InputSessionMessage

object InputSessionMessage:
  case object BeginSession extends InputSessionMessage
