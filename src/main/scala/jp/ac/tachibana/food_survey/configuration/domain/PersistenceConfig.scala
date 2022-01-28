package jp.ac.tachibana.food_survey.configuration.domain

// todo: secret utility class for showing configuration
case class PersistenceConfig(
  driver: String,
  url: String,
  user: String,
  password: String,
  connectionPoolSize: Int)
