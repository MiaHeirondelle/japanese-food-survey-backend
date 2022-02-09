package jp.ac.tachibana.food_survey.http.model.domain.session

import io.circe.{Encoder, JsonObject}

import jp.ac.tachibana.food_survey.domain.session.Session

sealed abstract class SessionJsonResponse(val status: SessionStatusJsonFormat)

object SessionJsonResponse:

  implicit val encoder: Encoder[SessionJsonResponse] =
    Encoder.AsObject.instance { r =>
      val base = r match {
        case SessionJsonResponse.NotCreated => JsonObject.empty
        case au: SessionJsonResponse.AwaitingUsers =>
          Encoder.AsObject[SessionJsonResponse.AwaitingUsers].encodeObject(au)
        case cb: SessionJsonResponse.CanBegin =>
          Encoder.AsObject[SessionJsonResponse.CanBegin].encodeObject(cb)
        case ip: SessionJsonResponse.InProgress =>
          Encoder.AsObject[SessionJsonResponse.InProgress].encodeObject(ip)
      }
      base.add("status", Encoder[SessionStatusJsonFormat].apply(r.status))
    }

  case object NotCreated extends SessionJsonResponse(SessionStatusJsonFormat.NotCreated)

  case class AwaitingUsers(
    joined_users: List[String],
    awaiting_users: List[String],
    admin: String)
      extends SessionJsonResponse(SessionStatusJsonFormat.AwaitingUsers)
      derives Encoder.AsObject

  case class CanBegin(
    joined_users: List[String],
    admin: String)
      extends SessionJsonResponse(SessionStatusJsonFormat.CanBegin)
      derives Encoder.AsObject

  case class InProgress(
    joined_users: List[String],
    admin: String)
      extends SessionJsonResponse(SessionStatusJsonFormat.InProgress)
      derives Encoder.AsObject

  def fromDomain(domainOpt: Option[Session]): SessionJsonResponse =
    domainOpt.fold(SessionJsonResponse.NotCreated) {
      case Session.AwaitingUsers(joinedUsers, waitingForUsers, admin) =>
        SessionJsonResponse.AwaitingUsers(
          joined_users = joinedUsers.map(_.id.value),
          awaiting_users = waitingForUsers.toList.map(_.id.value),
          admin = admin.id.value
        )
      case Session.CanBegin(joinedUsers, admin) =>
        SessionJsonResponse.CanBegin(
          joined_users = joinedUsers.toList.map(_.id.value),
          admin = admin.id.value
        )
      case Session.InProgress(joinedUsers, admin) =>
        SessionJsonResponse.InProgress(
          joined_users = joinedUsers.toList.map(_.id.value),
          admin = admin.id.value
        )
      case Session.Finished(joinedUsers, admin) =>
        SessionJsonResponse.NotCreated
    }
