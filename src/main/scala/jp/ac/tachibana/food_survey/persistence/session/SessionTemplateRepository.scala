package jp.ac.tachibana.food_survey.persistence.session

import jp.ac.tachibana.food_survey.domain.session.SessionTemplate

trait SessionTemplateRepository[F[_]]:

  def getActiveTemplate: F[SessionTemplate]
