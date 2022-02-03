package jp.ac.tachibana.food_survey.configuration.domain

import cats.MonadThrow
import cats.effect.Sync
import pureconfig.error.ConfigReaderException
import pureconfig.generic.derivation.ConfigReaderDerivation.Default.*
import pureconfig.{ConfigReader, ConfigSource}

import jp.ac.tachibana.food_survey.configuration.domain.http.{HttpAuthenticationConfig, HttpConfig}

case class ApplicationConfig(
  http: HttpConfig,
  persistence: PersistenceConfig)

object ApplicationConfig:

  implicit val reader: ConfigReader[ApplicationConfig] =
    ConfigReader.derived

  def load[F[_]: Sync]: F[ApplicationConfig] =
    Sync[F].defer {
      Sync[F]
        .fromEither(
          ConfigSource.default
            .load[ApplicationConfig]
            .left
            .map(ConfigReaderException[ApplicationConfig](_))
        )
    }
