package jp.ac.tachibana.food_survey.configuration.domain

import pureconfig.ConfigReader
import pureconfig.generic.derivation.ConfigReaderDerivation.Default.*

import jp.ac.tachibana.food_survey.configuration.ConfigImplicits.*
import jp.ac.tachibana.food_survey.util.crypto.Secret

case class PersistenceConfig(
  driver: String,
  url: String,
  user: Secret[String],
  password: Secret[String],
  connectionPoolSize: Int)

object PersistenceConfig:

  implicit val reader: ConfigReader[PersistenceConfig] =
    ConfigReader.derived
