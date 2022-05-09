package jp.ac.tachibana.food_survey.services.auth

import java.security.SecureRandom
import java.util.Base64
import cats.effect.Sync
import jp.ac.tachibana.food_survey.domain.auth.AuthToken

class DefaultAuthTokenGenerator[F[_]: Sync](
  random: SecureRandom,
  encoder: Base64.Encoder)
    extends AuthTokenGenerator[F]:

  override val generate: F[AuthToken] =
    Sync[F].delay {
      val bytes = Array.ofDim[Byte](64)
      random.nextBytes(bytes)
      AuthToken(encoder.encodeToString(bytes))
    }

object DefaultAuthTokenGenerator:

  def create[F[_]: Sync]: F[DefaultAuthTokenGenerator[F]] =
    Sync[F].delay {
      val random = new SecureRandom()
      val encoder = Base64.getUrlEncoder
      new DefaultAuthTokenGenerator[F](random, encoder)
    }
