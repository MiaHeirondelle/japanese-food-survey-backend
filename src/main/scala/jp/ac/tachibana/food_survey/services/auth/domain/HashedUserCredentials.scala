package jp.ac.tachibana.food_survey.services.auth.domain

import cats.FlatMap
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import jp.ac.tachibana.food_survey.domain.user
import jp.ac.tachibana.food_survey.domain.user.UserCredentials
import jp.ac.tachibana.food_survey.util.crypto.{CryptoHasher, Hash, Salt, Secret}

case class HashedUserCredentials(
  login: Secret[UserCredentials.Login],
  passwordHash: Hash,
  salt: Salt)
