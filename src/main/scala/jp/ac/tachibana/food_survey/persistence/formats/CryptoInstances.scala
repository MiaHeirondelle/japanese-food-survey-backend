package jp.ac.tachibana.food_survey.persistence.formats

import doobie.{Get, Meta, Put}

import jp.ac.tachibana.food_survey.util.crypto.{Hash, Salt, Secret}

trait CryptoInstances:

  implicit val hashMeta: Meta[Hash] = Meta[String].imap(Hash(_))(_.value)

  implicit val salt: Meta[Salt] = Meta[Array[Byte]].imap(Salt(_))(_.bytes)

  implicit def secretGet[A: Get]: Get[Secret[A]] =
    Get[A].tmap(Secret(_))

  implicit def secretGet[A: Put]: Put[Secret[A]] =
    Put[A].tcontramap(_.value)

object CryptoInstances extends CryptoInstances
