package jp.ac.tachibana.food_survey.persistence.util

import cats.data.NonEmptyList
import doobie.implicits.*
import doobie.postgres.circe.jsonb.implicits.*
import doobie.postgres.implicits.*
import doobie.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.session.Session.Status
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.persistence.util.SessionInstances.{SessionPostgresFormat, SessionStatePostgresFormat}
import jp.ac.tachibana.food_survey.persistence.util.UserInstances.*

trait SessionInstances:

  implicit val sessionNumberMeta: Meta[Session.Number] =
    Meta[Int].timap(Session.Number(_))(_.value)

  implicit val sessionStatusMeta: Meta[Session.Status] =
    pgEnumStringOpt[Session.Status](
      "session_status",
      {
        case "awaiting_users" => Some(Session.Status.AwaitingUsers)
        case "can_begin"      => Some(Session.Status.CanBegin)
        case "in_progress"    => Some(Session.Status.InProgress)
        case "finished"       => Some(Session.Status.Finished)
        case _                => None
      },
      {
        case Session.Status.AwaitingUsers => "awaiting_users"
        case Session.Status.CanBegin      => "can_begin"
        case Session.Status.InProgress    => "in_progress"
        case Session.Status.Finished      => "finished"
      }
    )

  implicit val sessionPostgresFormatRead: Read[SessionPostgresFormat] =
    // todo: fix toOption.get
    Read[(Session.Number, User.Id, Session.Status, Json)]
      .map { case (number, adminId, status, state) =>
        status match {
          case Session.Status.AwaitingUsers =>
            val decodedState = state.as[SessionStatePostgresFormat.AwaitingUsers].toOption.get
            SessionPostgresFormat.AwaitingUsers(
              number = number,
              joinedUsers = decodedState.joinedUsers,
              waitingForUsers = decodedState.waitingForUsers,
              admin = adminId
            )
          case Session.Status.CanBegin =>
            val decodedState = state.as[SessionStatePostgresFormat.CanBegin].toOption.get
            SessionPostgresFormat.CanBegin(
              number = number,
              joinedUsers = decodedState.joinedUsers,
              admin = adminId
            )
          case Session.Status.InProgress =>
            val decodedState = state.as[SessionStatePostgresFormat.InProgress].toOption.get
            SessionPostgresFormat.InProgress(
              number = number,
              joinedUsers = decodedState.joinedUsers,
              admin = adminId
            )
          case Session.Status.Finished =>
            val decodedState = state.as[SessionStatePostgresFormat.Finished].toOption.get
            SessionPostgresFormat.Finished(
              number = number,
              joinedUsers = decodedState.joinedUsers,
              admin = adminId
            )
        }
      }

  implicit val sessionPostgresFormatWrite: Write[SessionPostgresFormat] =
    Write[(Session.Number, User.Id, Session.Status, Json)]
      .contramap { s =>
        val encodedState = SessionStatePostgresFormat.encodeJson(s)
        (s.number, s.admin, s.status, encodedState)
      }

object SessionInstances extends SessionInstances:

  sealed abstract class SessionPostgresFormat(val status: Session.Status) {
    def number: Session.Number
    def admin: User.Id
  }

  object SessionPostgresFormat:

    extension (format: SessionPostgresFormat)
      def asStateJson: Json =
        SessionStatePostgresFormat.encodeJson(format)

    def fromDomain(session: Session): SessionPostgresFormat =
      session match {
        case s: Session.AwaitingUsers =>
          SessionPostgresFormat.AwaitingUsers(
            number = s.number,
            joinedUsers = s.joinedUsers.map(_.id),
            waitingForUsers = s.waitingForUsers.map(_.id),
            admin = s.admin.id
          )
        case s: Session.CanBegin =>
          SessionPostgresFormat.CanBegin(
            number = s.number,
            joinedUsers = s.joinedUsers.map(_.id),
            admin = s.admin.id
          )
        case s: Session.InProgress =>
          SessionPostgresFormat.InProgress(
            number = s.number,
            joinedUsers = s.joinedUsers.map(_.id),
            admin = s.admin.id
          )
        case s: Session.Finished =>
          SessionPostgresFormat.Finished(
            number = s.number,
            joinedUsers = s.joinedUsers.map(_.id),
            admin = s.admin.id
          )
      }

    case class AwaitingUsers(
      number: Session.Number,
      joinedUsers: List[User.Id],
      waitingForUsers: NonEmptyList[User.Id],
      admin: User.Id)
        extends SessionPostgresFormat(Session.Status.AwaitingUsers)

    case class CanBegin(
      number: Session.Number,
      joinedUsers: NonEmptyList[User.Id],
      admin: User.Id)
        extends SessionPostgresFormat(Session.Status.CanBegin)

    case class InProgress(
      number: Session.Number,
      joinedUsers: NonEmptyList[User.Id],
      admin: User.Id
      // todo: remaining questions (nel)
      // todo: replies
    ) extends SessionPostgresFormat(Session.Status.InProgress)

    case class Finished(
      number: Session.Number,
      joinedUsers: NonEmptyList[User.Id],
      admin: User.Id
      // todo: replies (nel)
    ) extends SessionPostgresFormat(Session.Status.Finished)

  sealed private[persistence] trait SessionStatePostgresFormat

  private[persistence] object SessionStatePostgresFormat:

    def encodeJson(format: SessionPostgresFormat): Json =
      format match {
        case s: SessionPostgresFormat.AwaitingUsers =>
          SessionStatePostgresFormat.AwaitingUsers(joinedUsers = s.joinedUsers, waitingForUsers = s.waitingForUsers).asJson
        case s: SessionPostgresFormat.CanBegin =>
          SessionStatePostgresFormat.CanBegin(joinedUsers = s.joinedUsers).asJson
        case s: SessionPostgresFormat.InProgress =>
          SessionStatePostgresFormat.InProgress(joinedUsers = s.joinedUsers).asJson
        case s: SessionPostgresFormat.Finished =>
          SessionStatePostgresFormat.Finished(joinedUsers = s.joinedUsers).asJson
      }

    case class AwaitingUsers(
      joinedUsers: List[User.Id],
      waitingForUsers: NonEmptyList[User.Id])
        extends SessionStatePostgresFormat
        derives Encoder.AsObject, Decoder

    case class CanBegin(joinedUsers: NonEmptyList[User.Id]) extends SessionStatePostgresFormat derives Encoder.AsObject, Decoder

    case class InProgress(
      joinedUsers: NonEmptyList[User.Id]
      // todo: remaining questions (nel)
      // todo: replies
    ) extends SessionStatePostgresFormat
        derives Encoder.AsObject, Decoder

    case class Finished(
      joinedUsers: NonEmptyList[User.Id]
      // todo: replies (nel)
    ) extends SessionStatePostgresFormat
        derives Encoder.AsObject, Decoder
