package jp.ac.tachibana.food_survey.domain.auth

import jp.ac.tachibana.food_survey.domain.user.UserCredentials
import jp.ac.tachibana.food_survey.util.crypto.{CryptoHasher, Hash, Salt, Secret}

case class HashedUserCredentials(
  login: Secret[UserCredentials.Login],
  passwordHash: Hash,
  passwordSalt: Salt)
