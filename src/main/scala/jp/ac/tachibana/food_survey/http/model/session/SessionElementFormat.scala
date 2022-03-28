package jp.ac.tachibana.food_survey.http.model.session

import io.circe.Encoder

import jp.ac.tachibana.food_survey.domain.session.SessionElement
import jp.ac.tachibana.food_survey.http.model.question.QuestionFormat

sealed abstract class SessionElementFormat(val `type`: SessionElementTypeFormat)

object SessionElementFormat:

  def fromDomain(element: SessionElement): SessionElementFormat =
    element match {
      case SessionElement.Question(number, question, _) =>
        SessionElementFormat.Question(
          number = number.value,
          question = QuestionFormat.fromDomain(question)
        )
    }

  implicit val encoder: Encoder[SessionElementFormat] =
    Encoder.AsObject.instance { se =>
      val base = se match {
        case q: SessionElementFormat.Question =>
          Encoder.AsObject[SessionElementFormat.Question].encodeObject(q)
      }
      base.add("type", Encoder[SessionElementTypeFormat].apply(se.`type`))
    }

  case class Question(
    number: Int,
    question: QuestionFormat)
      extends SessionElementFormat(SessionElementTypeFormat.Question)
      derives Encoder.AsObject
