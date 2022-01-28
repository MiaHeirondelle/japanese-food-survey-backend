package jp.ac.tachibana.food_survey.services.authentication

import jp.ac.tachibana.food_survey.services.authentication.domain.AuthToken

trait AuthTokenGenerator[F[_]]:

  def generate: F[AuthToken]
