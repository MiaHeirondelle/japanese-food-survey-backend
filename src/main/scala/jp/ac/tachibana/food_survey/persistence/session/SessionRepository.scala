package jp.ac.tachibana.food_survey.persistence.session

import jp.ac.tachibana.food_survey.domain.session.Session

trait SessionRepository[F[_]]:

  def getActiveSession: F[Option[Session]]
