package jp.ac.tachibana.food_survey.configuration.domain.authentication

case class AuthenticationConfig(
  domain: String,
  ssl: SSLConfig)
