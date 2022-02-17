package jp.ac.tachibana.food_survey.persistence.util

import cats.data.NonEmptyList
import doobie.postgres.implicits.*
import doobie.{Get, Meta, Put, Read}
import io.circe.{Decoder, Encoder, Json}

import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.session.Session.Status
import jp.ac.tachibana.food_survey.domain.user.User
import doobie.postgres.circe.jsonb.implicits.*
import doobie.implicits.*
import UserInstances.*

import jp.ac.tachibana.food_survey.domain.session.Session.Status.{AwaitingUsers, CanBegin, Finished, InProgress}
import jp.ac.tachibana.food_survey.persistence.util.SessionInstances.SessionPostgresFormat

trait SessionInstances:

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

// todo: session read, session write

object SessionInstances extends SessionInstances:

  sealed abstract class SessionPostgresFormat(val status: Session.Status)

  object SessionPostgresFormat:

    case class AwaitingUsers(
      waitingForUsers: NonEmptyList[User.Id],
      admin: User.Id)
        extends SessionPostgresFormat(Session.Status.AwaitingUsers)

    case class CanBegin(
      joinedUsers: NonEmptyList[User.Id],
      admin: User.Id)
        extends SessionPostgresFormat(Session.Status.CanBegin)

    case class InProgress(
      joinedUsers: NonEmptyList[User.Respondent],
      admin: User.Admin
      // todo: remaining questions (nel)
      // todo: replies
    ) extends SessionPostgresFormat(Session.Status.InProgress)

    case class Finished(
      joinedUsers: NonEmptyList[User.Id],
      admin: User.Id
      // todo: replies (nel)
    ) extends SessionPostgresFormat(Session.Status.Finished)

  sealed trait SessionStatePostgresFormat

  object SessionStatePostgresFormat:
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
