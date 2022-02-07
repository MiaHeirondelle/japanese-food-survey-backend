package jp.ac.tachibana.food_survey.persistence.domain.session

import cats.Applicative
import cats.syntax.applicative.*

import jp.ac.tachibana.food_survey.domain.session.Session

class PostgresSessionRepository[F[_]: Applicative] extends SessionRepository[F]:

  override def getActiveSession: F[Option[Session]] =
    None.pure[F]  
    
