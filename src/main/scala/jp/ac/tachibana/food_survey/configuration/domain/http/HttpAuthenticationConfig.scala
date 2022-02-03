package jp.ac.tachibana.food_survey.configuration.domain.http

import pureconfig.ConfigReader
import pureconfig.generic.derivation.ConfigReaderDerivation.Default.*

case class HttpAuthenticationConfig(
                                     domain: String,
                                     secure: HttpAuthenticationConfig.Mode)

object HttpAuthenticationConfig:

  implicit val reader: ConfigReader[HttpAuthenticationConfig] =
    ConfigReader.derived

  enum Mode:
    case Secure, Insecure

  object Mode:

    implicit val reader: ConfigReader[HttpAuthenticationConfig.Mode] =
      ConfigReader.booleanConfigReader.map {
        case true => HttpAuthenticationConfig.Mode.Secure
        case false => HttpAuthenticationConfig.Mode.Insecure
      }
