package jp.ac.tachibana.food_survey.domain.session

import cats.instances.int.catsKernelStdOrderForInt
import cats.{Order, Show}

import jp.ac.tachibana.food_survey.domain.question.Question as SessionQuestion

sealed trait SessionElement

object SessionElement:

  opaque type Number = Int

  object Number:

    val zero: SessionElement.Number = 0

    implicit val order: Order[SessionElement.Number] =
      catsKernelStdOrderForInt

    implicit val show: Show[SessionElement.Number] =
      Show.fromToString

    extension (number: Number)
      def increment: SessionElement.Number = number + 1
      def value: Int = number

    def apply(number: Int): SessionElement.Number = number

  // case class Text(title: String, text: String) extends SessionElement
  case class Question(
    number: SessionElement.Number,
    question: SessionQuestion)
      extends SessionElement
