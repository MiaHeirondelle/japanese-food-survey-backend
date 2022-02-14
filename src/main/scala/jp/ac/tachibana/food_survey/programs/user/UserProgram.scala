package jp.ac.tachibana.food_survey.programs.user

import jp.ac.tachibana.food_survey.domain.user.{User, UserCredentials}

trait UserProgram[F[_]]:

  def create(
    name: String,
    role: User.Role,
    credentials: UserCredentials): F[User.Id]
