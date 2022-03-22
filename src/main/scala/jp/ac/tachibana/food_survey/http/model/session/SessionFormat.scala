package jp.ac.tachibana.food_survey.http.model.session

import io.circe.{Encoder, JsonObject}

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.http.model.question.QuestionAnswerFormat
import jp.ac.tachibana.food_survey.http.model.user.UserFormat

sealed abstract class SessionFormat(val status: SessionStatusFormat)

object SessionFormat:

  implicit val encoder: Encoder[SessionFormat] =
    Encoder.AsObject.instance { r =>
      val base = r match {
        case SessionFormat.NotCreated => JsonObject.empty
        case au: SessionFormat.AwaitingUsers =>
          Encoder.AsObject[SessionFormat.AwaitingUsers].encodeObject(au)
        case cb: SessionFormat.CanBegin =>
          Encoder.AsObject[SessionFormat.CanBegin].encodeObject(cb)
        case ip: SessionFormat.InProgress =>
          Encoder.AsObject[SessionFormat.InProgress].encodeObject(ip)
      }
      base.add("status", Encoder[SessionStatusFormat].apply(r.status))
    }

  def fromDomainNotFinished(session: Session.NotFinished): SessionFormat =
    session match {
      case Session.AwaitingUsers(number, joinedUsers, waitingForUsers, admin) =>
        SessionFormat.AwaitingUsers(
          number = number.value,
          joined_users = joinedUsers.map(UserFormat.fromDomain),
          awaiting_users = waitingForUsers.toList.map(UserFormat.fromDomain),
          admin = UserFormat.fromDomain(admin)
        )
      case Session.CanBegin(number, joinedUsers, admin) =>
        SessionFormat.CanBegin(
          number = number.value,
          joined_users = joinedUsers.toList.map(UserFormat.fromDomain),
          admin = UserFormat.fromDomain(admin)
        )
      case Session.InProgress(number, joinedUsers, admin, answers, currentElementNumber, template) =>
        SessionFormat.InProgress(
          number = number.value,
          joined_users = joinedUsers.toList.map(UserFormat.fromDomain),
          admin = UserFormat.fromDomain(admin),
          answers = answers.toList.map(QuestionAnswerFormat.fromDomain),
          current_element_number = currentElementNumber.value,
          template = SessionTemplateFormat.fromDomain(template)
        )
    }

  def fromDomainNotFinishedOpt(domainOpt: Option[Session.NotFinished]): SessionFormat =
    domainOpt.fold(SessionFormat.NotCreated)(fromDomainNotFinished)

  case class AwaitingUsers(
    number: Int,
    joined_users: List[UserFormat],
    awaiting_users: List[UserFormat],
    admin: UserFormat)
      extends SessionFormat(SessionStatusFormat.AwaitingUsers)
      derives Encoder.AsObject

  case class CanBegin(
    number: Int,
    joined_users: List[UserFormat],
    admin: UserFormat)
      extends SessionFormat(SessionStatusFormat.CanBegin)
      derives Encoder.AsObject

  case class InProgress(
    number: Int,
    joined_users: List[UserFormat],
    admin: UserFormat,
    answers: List[QuestionAnswerFormat],
    current_element_number: Int,
    template: SessionTemplateFormat)
      extends SessionFormat(SessionStatusFormat.InProgress)
      derives Encoder.AsObject

  case object NotCreated extends SessionFormat(SessionStatusFormat.NotCreated)
