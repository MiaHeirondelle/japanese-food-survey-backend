package jp.ac.tachibana.food_survey.util.crypto

import java.math.BigInteger
import java.security.{MessageDigest, SecureRandom}

import cats.FlatMap
import cats.effect.Sync
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

import jp.ac.tachibana.food_survey.domain.user.UserCredentials
import jp.ac.tachibana.food_survey.services.auth.domain.HashedUserCredentials

class CryptoHasher[F[_]: Sync](
  random: SecureRandom,
  keyFactory: SecretKeyFactory):

  private val generateSalt: F[Salt] =
    Sync[F].delay {
      val bytes = Array.ofDim[Byte](16)
      random.nextBytes(bytes)
      Salt(bytes)
    }

  def computeHash(string: String): F[(Hash, Salt)] =
    for {
      salt <- generateSalt
      hash <- computeHash(string, salt)
    } yield (hash, salt)

  def verifyHash(
    string: String,
    hash: Hash,
    salt: Salt): F[Boolean] =
    computeHash(string, salt).map(_.value === hash.value)

  def computeHash(
    string: String,
    salt: Salt): F[Hash] =
    Sync[F].delay {
      val spec = new PBEKeySpec(string.toCharArray, salt.bytes, CryptoHasher.iterations, CryptoHasher.hashLength)
      Hash(keyFactory.generateSecret(spec).getEncoded)
    }

object CryptoHasher:

  private val iterations: Int = 10
  private val saltLength: Int = 16
  private val hashLength: Int = 64

  extension [F[_]: FlatMap](hasher: CryptoHasher[F])
    def hashCredentials(userCredentials: UserCredentials): F[HashedUserCredentials] =
      hasher
        .computeHash(userCredentials.password.value.value)
        .map { case (passwordHash, salt) =>
          HashedUserCredentials(
            login = userCredentials.login,
            passwordHash = passwordHash,
            passwordSalt = salt
          )
        }

  def create[F[_]: Sync]: F[CryptoHasher[F]] =
    Sync[F].delay {
      val digest = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
      val random = new SecureRandom()
      new CryptoHasher[F](random, digest)
    }
