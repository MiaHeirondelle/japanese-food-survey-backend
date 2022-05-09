package jp.ac.tachibana.food_survey.services.user

import java.util.UUID
import cats.effect.Sync
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import jp.ac.tachibana.food_survey.domain.user.{User, UserData}
import jp.ac.tachibana.food_survey.persistence.user.{UserDataRepository, UserRepository}

class DefaultUserService[F[_]: Sync](
  userRepository: UserRepository[F],
  userDataRepository: UserDataRepository[F])
    extends UserService[F]:

  override def create(
    name: String,
    role: User.Role): F[User] =
    generateUser(name, role).flatTap(userRepository.insert)

  override def getAllByRole(role: User.Role): F[List[User]] =
    userRepository.getAllByRole(role)

  override def saveUserData(userData: UserData): F[Unit] =
    userDataRepository.insert(userData)

  override def getUserData(userId: User.Id): F[Option[UserData]] =
    userDataRepository.get(userId)

  private def generateUser(
    name: String,
    role: User.Role): F[User] =
    generateUserId.map(User(_, name, role))

  private def generateUserId: F[User.Id] =
    Sync[F].delay {
      User.Id(UUID.randomUUID().toString)
    }
