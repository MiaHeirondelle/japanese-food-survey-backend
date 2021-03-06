package jp.ac.tachibana.food_survey.http.model.session

import cats.syntax.option.*
import io.circe.Encoder
import jp.ac.tachibana.food_survey.domain.session.SessionElement
import jp.ac.tachibana.food_survey.http.model.question.QuestionFormat

sealed abstract class SessionElementFormat(val `type`: SessionElementTypeFormat)

object SessionElementFormat:

  def fromDomain(element: SessionElement): SessionElementFormat =
    element match {
      case e: SessionElement.Question =>
        questionFromDomain(e)
      case e: SessionElement.QuestionReview =>
        questionReviewFromDomain(e)
      case e: SessionElement.Text =>
        textFromDomain(e)
    }

  private def questionFromDomain(element: SessionElement.Question): SessionElementFormat.Question =
    element match {
      case SessionElement.Question.Basic(number, question, _) =>
        SessionElementFormat.Question(
          number = number.value,
          question = QuestionFormat.fromDomain(question),
          previous_question = none
        )
      case SessionElement.Question.Repeated(number, question, previousQuestion, _) =>
        SessionElementFormat.Question(
          number = number.value,
          question = QuestionFormat.fromDomain(question),
          previous_question = QuestionFormat.fromDomain(previousQuestion).some
        )
    }

  private def questionReviewFromDomain(element: SessionElement.QuestionReview): SessionElementFormat.QuestionReview =
    element match {
      case SessionElement.QuestionReview.Basic(number, question, _) =>
        SessionElementFormat.QuestionReview(
          number = number.value,
          question = QuestionFormat.fromDomain(question),
          previous_question = none
        )

      case SessionElement.QuestionReview.Repeated(number, question, previousQuestion, _) =>
        SessionElementFormat.QuestionReview(
          number = number.value,
          question = QuestionFormat.fromDomain(question),
          previous_question = QuestionFormat.fromDomain(question).some
        )
    }

  private def textFromDomain(element: SessionElement.Text): SessionElementFormat =
    SessionElementFormat.Text(
      number = element.number.value,
      text = element.text
    )

  implicit val encoder: Encoder[SessionElementFormat] =
    Encoder.AsObject.instance { se =>
      val base = se match {
        case q: SessionElementFormat.Question =>
          Encoder.AsObject[SessionElementFormat.Question].encodeObject(q)
        case qr: SessionElementFormat.QuestionReview =>
          Encoder.AsObject[SessionElementFormat.QuestionReview].encodeObject(qr)
        case t: SessionElementFormat.Text =>
          Encoder.AsObject[SessionElementFormat.Text].encodeObject(t)
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
    question: QuestionFormat,
    previous_question: Option[QuestionFormat])
      extends SessionElementFormat(SessionElementTypeFormat.QuestionReview)
      derives Encoder.AsObject

  private case class Text(
    number: Int,
    text: String)
      extends SessionElementFormat(SessionElementTypeFormat.Text)
      derives Encoder.AsObject
