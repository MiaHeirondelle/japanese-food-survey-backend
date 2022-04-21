package jp.ac.tachibana.food_survey.http.model.session

import io.circe.Encoder

import jp.ac.tachibana.food_survey.domain.session.SessionElement
import jp.ac.tachibana.food_survey.http.model.question.QuestionFormat

sealed abstract class SessionElementFormat(val `type`: SessionElementTypeFormat)

object SessionElementFormat:

  def fromDomain(element: SessionElement): SessionElementFormat =
    element match {
      case e: SessionElement.Question =>
        questionFromDomain(e)
      case SessionElement.QuestionReview.Basic(number, question, _) =>
        SessionElementFormat.QuestionReview(number = number.value, question = QuestionFormat.fromDomain(question))
      case SessionElement.QuestionReview.Repeated(number, question, _) =>
        SessionElementFormat.QuestionReview(number = number.value, question = QuestionFormat.fromDomain(question))
    }

  private def questionFromDomain(element: SessionElement.Question): SessionElementFormat.Question =
    element match {
      case SessionElement.Question.Basic(number, question, _) =>
        SessionElementFormat.Question(
          number = number.value,
          question = QuestionFormat.fromDomain(question),
          previous_question = None
        )
      case SessionElement.Question.Repeated(number, question, previousQuestion, _) =>
        SessionElementFormat.Question(
          number = number.value,
          question = QuestionFormat.fromDomain(question),
          previous_question = Some(QuestionFormat.fromDomain(previousQuestion))
        )
    }

  private def questionReviewFromDomain(element: SessionElement.QuestionReview): SessionElementFormat.QuestionReview =
    element match {
      case SessionElement.QuestionReview.Basic(number, question, _) =>
        SessionElementFormat.QuestionReview(number = number.value, question = QuestionFormat.fromDomain(question))
      case SessionElement.QuestionReview.Repeated(number, question, _) =>
        SessionElementFormat.QuestionReview(
          number = number.value,
          question = QuestionFormat.fromDomain(question)
        )
    }

  implicit val encoder: Encoder[SessionElementFormat] =
    Encoder.AsObject.instance { se =>
      val base = se match {
        case q: SessionElementFormat.Question =>
          Encoder.AsObject[SessionElementFormat.Question].encodeObject(q)
        case qr: SessionElementFormat.QuestionReview =>
          Encoder.AsObject[SessionElementFormat.QuestionReview].encodeObject(qr)
      }
      base.add("type", Encoder[SessionElementTypeFormat].apply(se.`type`))
    }

  private case class Question(
    number: Int,
    question: QuestionFormat,
    previous_question: Option[QuestionFormat])
      extends SessionElementFormat(SessionElementTypeFormat.Question)
      derives Encoder.AsObject

  private case class QuestionReview(
    number: Int,
    question: QuestionFormat)
      extends SessionElementFormat(SessionElementTypeFormat.QuestionReview)
      derives Encoder.AsObject
