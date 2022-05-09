package jp.ac.tachibana.food_survey.services.auth

import jp.ac.tachibana.food_survey.domain.auth.AuthToken

trait AuthTokenGenerator[F[_]]:

  def generate: F[AuthToken]
