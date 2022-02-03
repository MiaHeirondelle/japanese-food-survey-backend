package jp.ac.tachibana.food_survey.configuration.domain.http

import org.http4s.headers.Origin
import pureconfig.ConfigReader
import pureconfig.error.{CannotConvert, FailureReason}
import pureconfig.generic.derivation.ConfigReaderDerivation.Default.*

case class HttpConfig(
  host: String,
  port: Int,
  cors: CorsConfig,
  authentication: HttpAuthenticationConfig)

object HttpConfig:

  implicit val reader: ConfigReader[HttpConfig] =
    ConfigReader.derived
