package jp.ac.tachibana.food_survey.persistence.auth

import jp.ac.tachibana.food_survey.domain.user
import jp.ac.tachibana.food_survey.domain.user.{User, UserCredentials}
import jp.ac.tachibana.food_survey.services.auth.domain.HashedUserCredentials
import jp.ac.tachibana.food_survey.util.crypto.{Hash, Salt, Secret}

trait CredentialsRepository[F[_]]:

  def insert(credentials: CredentialsRepository.StoredCredentials): F[Unit]

  def getByLogin(login: UserCredentials.Login): F[Option[CredentialsRepository.StoredCredentials]]

object CredentialsRepository:

  case class StoredCredentials(
    userId: User.Id,
    login: Secret[UserCredentials.Login],
    passwordHash: Hash,
    salt: Salt)

  object StoredCredentials:

    def fromHashedCredentials(
      userId: User.Id,
      hashedCredentials: HashedUserCredentials): StoredCredentials =
      StoredCredentials(
        userId = userId,
        login = hashedCredentials.login,
        passwordHash = hashedCredentials.passwordHash,
        salt = hashedCredentials.salt
      )

    extension (storedCredentials: StoredCredentials)
      def toHashedCredentials: HashedUserCredentials =
        HashedUserCredentials(storedCredentials.login, storedCredentials.passwordHash, storedCredentials.salt)
