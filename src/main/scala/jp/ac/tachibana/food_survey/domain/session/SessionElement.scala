package jp.ac.tachibana.food_survey.domain.session

import scala.concurrent.duration.FiniteDuration

import cats.instances.int.catsKernelStdOrderForInt
import cats.{Order, Show}

import jp.ac.tachibana.food_survey.domain.question.Question as SessionQuestion

sealed trait SessionElement:
  def number: SessionElement.Number
  def showDuration: FiniteDuration

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

  sealed trait Question extends SessionElement:
    def question: SessionQuestion

  object Question:

    case class Basic(
      number: SessionElement.Number,
      question: SessionQuestion.Basic,
      showDuration: FiniteDuration)
        extends SessionElement.Question

    case class Repeated(
      number: SessionElement.Number,
      question: SessionQuestion.Repeated,
      previousQuestion: SessionQuestion,
      showDuration: FiniteDuration)
        extends SessionElement.Question

  sealed trait QuestionReview extends SessionElement:
    def question: SessionQuestion

  object QuestionReview:

    case class Basic(
      number: SessionElement.Number,
      question: SessionQuestion.Basic,
      showDuration: FiniteDuration)
        extends SessionElement.QuestionReview

    case class Repeated(
      number: SessionElement.Number,
      question: SessionQuestion.Repeated,
      previousQuestion: SessionQuestion,
      showDuration: FiniteDuration)
        extends SessionElement.QuestionReview
