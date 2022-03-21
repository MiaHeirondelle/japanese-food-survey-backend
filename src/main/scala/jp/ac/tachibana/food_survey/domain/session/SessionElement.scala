package jp.ac.tachibana.food_survey.domain.session

import cats.Order
import cats.instances.int.catsKernelStdOrderForInt

import jp.ac.tachibana.food_survey.domain.question.Question as SessionQuestion

trait SessionElement

object SessionElement:

  opaque type Number = Int

  object Number:

    val zero: Number = 0

    implicit val order: Order[Number] =
      catsKernelStdOrderForInt

    extension (number: Number)
      def increment: Number = number + 1
      def value: Int = number

    def apply(number: Int): Number = number

  // case class Text(title: String, text: String) extends SessionElement
  case class Question(
    number: SessionElement.Number,
    question: SessionQuestion)
      extends SessionElement
