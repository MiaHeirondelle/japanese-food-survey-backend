package jp.ac.tachibana.food_survey.util.crypto

opaque type Salt = Array[Byte]

object Salt:

  def apply(string: String): Salt = string.getBytes

  def apply(bytes: Array[Byte]): Salt = bytes

  extension (salt: Salt) def bytes: Array[Byte] = salt
