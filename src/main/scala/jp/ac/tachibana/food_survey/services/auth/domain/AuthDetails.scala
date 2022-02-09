package jp.ac.tachibana.food_survey.services.auth.domain

import jp.ac.tachibana.food_survey.domain.user.User

case class AuthDetails(
  token: AuthToken,
  user: User)
