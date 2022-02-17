package jp.ac.tachibana.food_survey.services.session

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.persistence.session.SessionRepository

class DefaultSessionService[F[_]](sessionRepository: SessionRepository[F]) extends SessionService[F]:

  override def getActiveSession: F[Option[Session]] =
    sessionRepository.getActiveSession
