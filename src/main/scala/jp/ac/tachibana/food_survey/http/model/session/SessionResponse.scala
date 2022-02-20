package jp.ac.tachibana.food_survey.http.model.session

import io.circe.{Encoder, JsonObject}

import jp.ac.tachibana.food_survey.domain.session.Session

sealed abstract class SessionResponse(val status: SessionStatusFormat)

object SessionResponse:

  implicit val encoder: Encoder[SessionResponse] =
    Encoder.AsObject.instance { r =>
      val base = r match {
        case SessionResponse.NotCreated => JsonObject.empty
        case au: SessionResponse.AwaitingUsers =>
          Encoder.AsObject[SessionResponse.AwaitingUsers].encodeObject(au)
        case cb: SessionResponse.CanBegin =>
          Encoder.AsObject[SessionResponse.CanBegin].encodeObject(cb)
        case ip: SessionResponse.InProgress =>
          Encoder.AsObject[SessionResponse.InProgress].encodeObject(ip)
      }
      base.add("status", Encoder[SessionStatusFormat].apply(r.status))
    }

  def fromDomain(domainOpt: Option[Session]): SessionResponse =
    domainOpt.fold(SessionResponse.NotCreated) {
      case Session.AwaitingUsers(number, joinedUsers, waitingForUsers, admin) =>
        SessionResponse.AwaitingUsers(
          number = number.value,
          joined_users = joinedUsers.map(_.id.value),
          awaiting_users = waitingForUsers.toList.map(_.id.value),
          admin = admin.id.value
        )
      case Session.CanBegin(number, joinedUsers, admin) =>
        SessionResponse.CanBegin(
          number = number.value,
          joined_users = joinedUsers.toList.map(_.id.value),
          admin = admin.id.value
        )
      case Session.InProgress(number, joinedUsers, admin) =>
        SessionResponse.InProgress(
          number = number.value,
          joined_users = joinedUsers.toList.map(_.id.value),
          admin = admin.id.value
        )
      case _: Session.Finished =>
        SessionResponse.NotCreated
    }

  case class AwaitingUsers(
    number: Int,
    joined_users: List[String],
    awaiting_users: List[String],
    admin: String)
      extends SessionResponse(SessionStatusFormat.AwaitingUsers)
      derives Encoder.AsObject

  case class CanBegin(
    number: Int,
    joined_users: List[String],
    admin: String)
      extends SessionResponse(SessionStatusFormat.CanBegin)
      derives Encoder.AsObject

  case class InProgress(
    number: Int,
    joined_users: List[String],
    admin: String)
      extends SessionResponse(SessionStatusFormat.InProgress)
      derives Encoder.AsObject

  case object NotCreated extends SessionResponse(SessionStatusFormat.NotCreated)
