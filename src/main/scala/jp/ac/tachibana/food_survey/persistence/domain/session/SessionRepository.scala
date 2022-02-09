package jp.ac.tachibana.food_survey.persistence.domain.session

import jp.ac.tachibana.food_survey.domain.session.Session

trait SessionRepository[F[_]]:

  def getActiveSession: F[Option[Session]]
