package jp.ac.tachibana.food_survey.domain.question

import cats.Eq

import jp.ac.tachibana.food_survey.domain.question.Question.ScaleText

sealed trait Question:
  def id: Question.Id
  def text: String
  def scaleText: ScaleText

object Question:

  case class ScaleText(
    minBoundCaption: String,
    maxBoundCaption: String)

  opaque type Id = String

  object Id:

    implicit val eq: Eq[Question.Id] =
      Eq.fromUniversalEquals

    def apply(questionId: String): Question.Id = questionId

    extension (questionId: Question.Id) def value: String = questionId

  case class Basic(
    id: Question.Id,
    text: String,
    scaleText: Question.ScaleText)
      extends Question

  case class Repeated(
    id: Question.Id,
    previousQuestionId: Question.Id,
    text: String,
    scaleText: Question.ScaleText)
      extends Question
