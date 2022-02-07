package jp.ac.tachibana.food_survey.services.domain.session

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.persistence.domain.session.SessionRepository

class DefaultSessionService[F[_]](sessionRepository: SessionRepository[F]) extends SessionService[F]:

  override def getActiveSession: F[Option[Session]] =
    sessionRepository.getActiveSession