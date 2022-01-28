package jp.ac.tachibana.food_survey.util.crypto

import java.io.FileInputStream
import java.security.{KeyStore, SecureRandom}
import cats.syntax.applicative.*

import scala.util.Using

import cats.effect.Sync
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import jp.ac.tachibana.food_survey.configuration.domain.authentication.SSLConfig

object SSLContextLoader:

  private val keyStoreFile = "ssl/keystore.jks"

  def load[F[_]: Sync](sslConfig: SSLConfig): F[Option[SSLContext]] =
    sslConfig match {
      case SSLConfig.SSLDisabled =>
        None.pure[F]

      case SSLConfig.SSLEnabled(keyStorePassword, keyManagerPassword) =>
        Sync[F].defer {
          val keyStorePath = getClass.getClassLoader.getResource(keyStoreFile).getPath
          Sync[F].fromTry(
            Using(new FileInputStream(keyStorePath)) { keyStoreFile =>
              val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
              keyStore.load(keyStoreFile, keyStorePassword.toCharArray)

              val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
              keyManagerFactory.init(keyStore, keyManagerPassword.toCharArray)

              val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
              trustManagerFactory.init(keyStore)

              val sslContext = SSLContext.getInstance("TLS")
              sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom())
              Some(sslContext)
            }
          )
        }
    }
