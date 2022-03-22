package jp.ac.tachibana.food_survey.http.model.session

import io.circe.Encoder

import jp.ac.tachibana.food_survey.domain.session.SessionTemplate

case class SessionTemplateFormat(elements: List[SessionElementFormat]) derives Encoder.AsObject

object SessionTemplateFormat:

  def fromDomain(sessionTemplate: SessionTemplate): SessionTemplateFormat =
    SessionTemplateFormat(sessionTemplate.elements.toList.map(SessionElementFormat.fromDomain))
