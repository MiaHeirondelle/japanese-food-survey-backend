package jp.ac.tachibana.food_survey.services.auth.domain

import cats.FlatMap
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import jp.ac.tachibana.food_survey.domain.user.UserCredentials
import jp.ac.tachibana.food_survey.util.crypto.{CryptoHasher, Hash, Salt}

case class HashedUserCredentials(
  loginHash: Hash,
  passwordHash: Hash,
  salt: Salt)

object HashedUserCredentials:

  def fromCredentials[F[_]: FlatMap](hasher: CryptoHasher[F])(credentials: UserCredentials): F[HashedUserCredentials] =
    for {
      (loginHash, salt) <- hasher.computeHash(credentials.login.value.value)
      passwordHash <- hasher.computeHash(credentials.password.value.value, salt)
    } yield HashedUserCredentials(loginHash, passwordHash, salt)
