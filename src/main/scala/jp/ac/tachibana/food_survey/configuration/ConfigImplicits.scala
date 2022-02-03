package jp.ac.tachibana.food_survey.configuration

import com.typesafe.config.ConfigValueFactory
import pureconfig.generic.derivation.ConfigReaderDerivation.Default.*
import pureconfig.{ConfigCursor, ConfigReader}

private[configuration] object ConfigImplicits:

  private def stringCsvListReader[A: ConfigReader]: ConfigReader[List[A]] =
    (cur: ConfigCursor) =>
      for {
        str <- ConfigReader.stringConfigReader.from(cur)
        values <- ConfigReader.Result.sequence(
          str
            .split(",")
            .toList
            .map(s => ConfigReader[A].from(ConfigValueFactory.fromAnyRef(s))))
      } yield values

  implicit def listReader[A: ConfigReader]: ConfigReader[List[A]] =
    ConfigReader.derived[List[A]].orElse(stringCsvListReader)

  implicit def setReader[A: ConfigReader]: ConfigReader[Set[A]] =
    listReader.map(_.toSet)
