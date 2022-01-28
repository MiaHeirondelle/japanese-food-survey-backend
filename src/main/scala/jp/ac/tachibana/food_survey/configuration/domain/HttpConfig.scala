package jp.ac.tachibana.food_survey.configuration.domain

import pureconfig.ConfigReader
import pureconfig.generic.derivation.ConfigReaderDerivation.Default.*

case class HttpConfig(
  host: String,
  port: Int)

object HttpConfig:

  implicit val reader: ConfigReader[HttpConfig] =
    ConfigReader.derived
