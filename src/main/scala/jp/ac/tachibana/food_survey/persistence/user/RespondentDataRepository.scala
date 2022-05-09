package jp.ac.tachibana.food_survey.persistence.user

import jp.ac.tachibana.food_survey.domain.user.{RespondentData, User}

trait RespondentDataRepository[F[_]]:

  def insert(respondentData: RespondentData): F[Unit]

  def checkRespondentDataExists(userId: User.Id): F[Boolean]
