package jp.ac.tachibana.food_survey.persistence.util

import doobie.Meta

import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.util.crypto.Hash

object ParameterInstances:

  implicit val hashMeta: Meta[Hash] = Meta[String].imap(Hash(_))(_.value)

  implicit val userIdMeta: Meta[User.Id] = Meta[String].imap(User.Id(_))(_.value)
