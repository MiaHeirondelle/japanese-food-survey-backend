package jp.ac.tachibana.food_survey.services.authentication.domain

opaque type AuthToken = String

object AuthToken:

  def apply(value: String): AuthToken = value

  extension (token: AuthToken) def value: String = token
