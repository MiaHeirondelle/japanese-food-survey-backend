package jp.ac.tachibana.food_survey.configuration.domain.authentication

import pureconfig.ConfigReader
import pureconfig.generic.derivation.ConfigReaderDerivation.Default.*

sealed trait SSLConfig

object SSLConfig:

  case object SSLDisabled extends SSLConfig

  // todo: secret
  case class SSLEnabled(
    keyStorePassword: String,
    keyManagerPassword: String)
      extends SSLConfig

  object SSLEnabled:
    implicit val reader: ConfigReader[SSLConfig.SSLEnabled] =
      ConfigReader.derived

  implicit val reader: ConfigReader[SSLConfig] =
    ConfigReader.fromCursor(cursor =>
      for {
        objectCursor <- cursor.asObjectCursor
        enabledCursor <- objectCursor.atKey("enabled")
        enabled <- enabledCursor.asBoolean
        result <-
          if (enabled)
            SSLConfig.SSLEnabled.reader.from(cursor)
          else
            Right(SSLConfig.SSLDisabled)
      } yield result)
