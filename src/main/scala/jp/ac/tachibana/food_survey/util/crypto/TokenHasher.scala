package jp.ac.tachibana.food_survey.util.crypto

import java.math.BigInteger
import java.security.MessageDigest
import cats.effect.Sync
import jp.ac.tachibana.food_survey.domain.auth.AuthToken

class TokenHasher[F[_]: Sync]:

  def hash(token: AuthToken): F[Hash] =
    Sync[F].delay {
      val digest = MessageDigest.getInstance("SHA-256")
      Hash(digest.digest(token.value.getBytes("utf8")))
    }
