package jp.ac.tachibana.food_survey.util.crypto

opaque type Salt = Array[Byte]

object Salt:

  def apply(bytes: Array[Byte]): Salt =
    bytes

  extension (salt: Salt)
    def bytes: Array[Byte] =
      salt

    def hexString: String =
      salt.map("%02X".format(_)).mkString
