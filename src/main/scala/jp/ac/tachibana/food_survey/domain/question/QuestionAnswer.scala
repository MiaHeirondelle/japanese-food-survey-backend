package jp.ac.tachibana.food_survey.domain.question

import jp.ac.tachibana.food_survey.domain.question.Question
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User

sealed trait QuestionAnswer:
  def sessionNumber: Session.Number
  def questionId: Question.Id
  def respondentId: User.Id
  def value: QuestionAnswer.ScaleValue
  def comment: QuestionAnswer.Comment

object QuestionAnswer:

  opaque type ScaleValue = Byte
  opaque type Comment = String

  case class Basic(
    sessionNumber: Session.Number,
    questionId: Question.Id,
    respondentId: User.Id,
    value: QuestionAnswer.ScaleValue,
    comment: QuestionAnswer.Comment)
      extends QuestionAnswer

  case class Repeated(
    sessionNumber: Session.Number,
    questionId: Question.Id,
    respondentId: User.Id,
    value: QuestionAnswer.ScaleValue,
    comment: QuestionAnswer.Comment,
    previousQuestionId: Question.Id)
      extends QuestionAnswer
