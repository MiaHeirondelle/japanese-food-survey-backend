package jp.ac.tachibana.food_survey.persistence

import cats.effect.{Async, Resource, Sync}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.Transactor
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration

import jp.ac.tachibana.food_survey.configuration.domain.PersistenceConfig

object DatabaseTransactor:

  def start[F[_]: Async](config: PersistenceConfig): Resource[F, Transactor[F]] =
    for {
      ds <- createDatasource(config)
      tr <- createTransactor(ds, config.connectionPoolSize)
      _ <- Resource.eval(runMigrations(ds))
    } yield tr

  private def createDatasource[F[_]: Sync](config: PersistenceConfig): Resource[F, HikariDataSource] =
    Resource.make(Sync[F].delay {
      val hikariConfig = new HikariConfig()
      hikariConfig.setDriverClassName(config.driver)
      hikariConfig.setJdbcUrl(config.url)
      hikariConfig.setUsername(config.user)
      hikariConfig.setPassword(config.password)
      new HikariDataSource(hikariConfig)
    })(ds => Sync[F].delay(ds.close()))

  private def createTransactor[F[_]: Async](
    dataSource: HikariDataSource,
    connectionPoolSize: Int): Resource[F, Transactor[F]] =
    ExecutionContexts
      .fixedThreadPool[F](connectionPoolSize)
      .map(HikariTransactor(dataSource, _))

  private def runMigrations[F[_]: Sync](dataSource: DataSource): F[Unit] =
    Sync[F].delay {
      Flyway
        .configure()
        .configuration(
          new FluentConfiguration()
            .dataSource(dataSource)
        )
        .load()
        .migrate()
    }
