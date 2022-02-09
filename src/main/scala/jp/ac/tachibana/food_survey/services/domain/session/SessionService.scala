package jp.ac.tachibana.food_survey.services.domain.session

import jp.ac.tachibana.food_survey.domain.session.Session

trait SessionService[F[_]]:

  def getActiveSession: F[Option[Session]]
