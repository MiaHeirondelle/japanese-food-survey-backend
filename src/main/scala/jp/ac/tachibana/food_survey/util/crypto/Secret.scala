package jp.ac.tachibana.food_survey.util.crypto

import java.security.MessageDigest

case class Secret[A](value: A) extends AnyVal:

  override def toString: String = {
    val digest = Secret.digest
    val bytes = digest.digest(value.toString.getBytes())
    Hash(bytes).value
  }

object Secret:

  private def digest = MessageDigest.getInstance("MD5")
