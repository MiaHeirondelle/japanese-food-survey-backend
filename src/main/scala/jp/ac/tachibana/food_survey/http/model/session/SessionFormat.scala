package jp.ac.tachibana.food_survey.http.model.session

import io.circe.{Encoder, JsonObject}

import jp.ac.tachibana.food_survey.domain.session.Session
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

  def fromDomain(session: Session): SessionFormat =
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
      case Session.InProgress(number, joinedUsers, admin) =>
        SessionFormat.InProgress(
          number = number.value,
          joined_users = joinedUsers.toList.map(UserFormat.fromDomain),
          admin = UserFormat.fromDomain(admin)
        )
      case _: Session.Finished =>
        SessionFormat.NotCreated
    }

  def fromDomainOpt(domainOpt: Option[Session]): SessionFormat =
    domainOpt.fold(SessionFormat.NotCreated)(fromDomain)

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
    admin: UserFormat)
      extends SessionFormat(SessionStatusFormat.InProgress)
      derives Encoder.AsObject

  case object NotCreated extends SessionFormat(SessionStatusFormat.NotCreated)
