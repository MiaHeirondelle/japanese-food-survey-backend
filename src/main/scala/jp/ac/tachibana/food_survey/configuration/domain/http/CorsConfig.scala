package jp.ac.tachibana.food_survey.configuration.domain.http

import org.http4s.headers.Origin
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import pureconfig.generic.derivation.ConfigReaderDerivation.Default.*

import jp.ac.tachibana.food_survey.configuration.ConfigImplicits.*

sealed trait CorsConfig

object CorsConfig:

  case class CorsEnabled(allowedOrigins: Set[Origin]) extends CorsConfig

  case object CorsDisabled extends CorsConfig

  object CorsEnabled:
    implicit private val originReader: ConfigReader[Origin] =
      ConfigReader.stringConfigReader.emap(value => Origin.parse(value).left.map(e => CannotConvert(value, "Origin", e.message)))

    implicit val reader: ConfigReader[CorsConfig.CorsEnabled] =
      ConfigReader.derived

  implicit val reader: ConfigReader[CorsConfig] =
    ConfigReader.fromCursor(cursor =>
      for {
        objectCursor <- cursor.asObjectCursor
        enabledCursor <- objectCursor.atKey("enabled")
        enabled <- enabledCursor.asBoolean
        result <-
          if (enabled)
            CorsConfig.CorsEnabled.reader.from(cursor)
          else
            Right(CorsConfig.CorsDisabled)
      } yield result)
