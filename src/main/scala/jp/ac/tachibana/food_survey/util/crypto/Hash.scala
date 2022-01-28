package jp.ac.tachibana.food_survey.util.crypto

import java.math.BigInteger

opaque type Hash = String

object Hash:

  def apply(string: String): Hash = string

  def apply(bytes: Array[Byte]): Hash =
    String.format("%064x", new BigInteger(bytes).abs())

  extension (hash: Hash) def value: String = hash
