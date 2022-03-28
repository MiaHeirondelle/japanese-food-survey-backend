package jp.ac.tachibana.food_survey.persistence.formats

import scala.concurrent.duration.*

import cats.Show
import cats.data.NonEmptyList
import cats.syntax.show.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.circe.jsonb.implicits.*
import doobie.postgres.implicits.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}

import jp.ac.tachibana.food_survey.domain.question.Question as SessionQuestion
import jp.ac.tachibana.food_survey.domain.session.{Session, SessionElement}
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.persistence.formats.QuestionInstances.*
import jp.ac.tachibana.food_survey.persistence.formats.SessionInstances.{SessionElementPostgresFormat, SessionPostgresFormat, SessionStatePostgresFormat}
import jp.ac.tachibana.food_survey.persistence.formats.UserInstances.*

trait SessionInstances:

  implicit val sessionNumberMeta: Meta[Session.Number] =
    Meta[Int].timap(Session.Number(_))(_.value)

  implicit val sessionElementNumberMeta: Meta[SessionElement.Number] =
    Meta[Int].timap(SessionElement.Number(_))(_.value)

  implicit val sessionPostgresFormatStatusMeta: Meta[SessionPostgresFormat.Status] =
    pgEnumStringOpt[SessionPostgresFormat.Status](
      "session_status",
      {
        case "awaiting_users" => Some(SessionPostgresFormat.Status.AwaitingUsers)
        case "finished"       => Some(SessionPostgresFormat.Status.Finished)
        case _                => None
      },
      {
        case SessionPostgresFormat.Status.AwaitingUsers => "awaiting_users"
        case SessionPostgresFormat.Status.Finished      => "finished"
      }
    )

  implicit val sessionElementPostgresFormatTypeMeta: Meta[SessionElementPostgresFormat.Type] =
    pgEnumStringOpt(
      "session_element_type",
      {
        case "question" => Some(SessionElementPostgresFormat.Type.Question)
        case "review"   => Some(SessionElementPostgresFormat.Type.Review)
        case "text"     => Some(SessionElementPostgresFormat.Type.Text)
        case _          => None
      },
      {
        case SessionElementPostgresFormat.Type.Question => "question"
        case SessionElementPostgresFormat.Type.Review   => "review"
        case SessionElementPostgresFormat.Type.Text     => "text"
      }
    )

  implicit val activeSessionPostgresFormatStatusMeta: Meta[SessionPostgresFormat.Status.AwaitingUsers.type] =
    new Meta(
      sessionPostgresFormatStatusMeta.get.temap {
        case SessionPostgresFormat.Status.AwaitingUsers => Right(SessionPostgresFormat.Status.AwaitingUsers)
        case s @ SessionPostgresFormat.Status.Finished  => Left(show"Incorrect session status value $s")
      },
      sessionPostgresFormatStatusMeta.put.contramap(x => x)
    )

  implicit val sessionPostgresFormatRead: Read[SessionPostgresFormat.AwaitingUsers] =
    // todo: fix toOption.get
    Read[(Session.Number, User.Id, SessionPostgresFormat.Status.AwaitingUsers.type, Json)]
      .map { case (number, adminId, _, state) =>
        val decodedState = state.as[SessionStatePostgresFormat.AwaitingUsers].toOption.get
        SessionPostgresFormat.AwaitingUsers(
          number = number,
          admin = adminId
        )
      }

  implicit val sessionPostgresFormatWrite: Write[SessionPostgresFormat] =
    Write[(Session.Number, User.Id, SessionPostgresFormat.Status, Json)]
      .contramap { s =>
        val encodedState = SessionStatePostgresFormat.encodeJson(s)
        val encodedStatus = SessionPostgresFormat.Status.fromDomain(s.status)
        (s.number, s.admin, encodedStatus, encodedState)
      }

  implicit val sessionElementPostgresFormatRead: Read[SessionElementPostgresFormat] =
    Read[(SessionElement.Number, SessionElementPostgresFormat.Type, Option[SessionQuestion.Id], Int)]
      .map {
        case (number, SessionElementPostgresFormat.Type.Question, Some(questionId), showDurationSeconds) =>
          SessionElementPostgresFormat.Question(number, questionId, showDurationSeconds.seconds)

        case (number, _, _, _) =>
          throw new Exception(show"Invalid session element: $number")
      }

object SessionInstances extends SessionInstances:

  sealed abstract class SessionPostgresFormat(
    val status: Session.Status,
    val encodedStatus: SessionPostgresFormat.Status) {
    def number: Session.Number
    def admin: User.Id
  }

  object SessionPostgresFormat:

    enum Status:
      case AwaitingUsers, Finished

    object Status:

      implicit val show: Show[SessionPostgresFormat.Status] =
        Show.fromToString

      def fromDomain(status: Session.Status): SessionPostgresFormat.Status =
        status match {
          case Session.Status.AwaitingUsers =>
            SessionPostgresFormat.Status.AwaitingUsers
          case Session.Status.CanBegin =>
            SessionPostgresFormat.Status.AwaitingUsers
          case Session.Status.InProgress =>
            SessionPostgresFormat.Status.AwaitingUsers
          case Session.Status.Finished =>
            SessionPostgresFormat.Status.Finished
        }

    extension (format: SessionPostgresFormat)
      def asStateJson: Json =
        SessionStatePostgresFormat.encodeJson(format)

    def fromDomain(session: Session): SessionPostgresFormat =
      session match {
        case s: Session.AwaitingUsers =>
          SessionPostgresFormat.AwaitingUsers(
            number = s.number,
            admin = s.admin.id
          )
        case s: Session.CanBegin =>
          SessionPostgresFormat.AwaitingUsers(
            number = s.number,
            admin = s.admin.id
          )
        case s: Session.InProgress =>
          SessionPostgresFormat.AwaitingUsers(
            number = s.number,
            admin = s.admin.id
          )
        case s: Session.Finished =>
          SessionPostgresFormat.Finished(
            number = s.number,
            admin = s.admin.id
          )
      }

    case class AwaitingUsers(
      number: Session.Number,
      admin: User.Id)
        extends SessionPostgresFormat(Session.Status.AwaitingUsers, SessionPostgresFormat.Status.AwaitingUsers)

    case class Finished(
      number: Session.Number,
      admin: User.Id
      // todo: replies (nel)
    ) extends SessionPostgresFormat(Session.Status.Finished, SessionPostgresFormat.Status.Finished)

  sealed private[persistence] trait SessionStatePostgresFormat

  private[persistence] object SessionStatePostgresFormat:

    def encodeJson(format: SessionPostgresFormat): Json =
      format match {
        case s: SessionPostgresFormat.AwaitingUsers =>
          SessionStatePostgresFormat.AwaitingUsers().asJson
        case s: SessionPostgresFormat.Finished =>
          SessionStatePostgresFormat.Finished().asJson
      }

    case class AwaitingUsers() extends SessionStatePostgresFormat derives Encoder.AsObject, Decoder

    case class Finished(
      // todo: replies (nel)
    ) extends SessionStatePostgresFormat
        derives Encoder.AsObject, Decoder

  sealed abstract private[persistence] class SessionElementPostgresFormat(val `type`: SessionElementPostgresFormat.Type):
    def number: SessionElement.Number
    def showDuration: FiniteDuration

  private[persistence] object SessionElementPostgresFormat:
    case class Question(
      number: SessionElement.Number,
      questionId: SessionQuestion.Id,
      showDuration: FiniteDuration)
        extends SessionElementPostgresFormat(SessionElementPostgresFormat.Type.Question)

    enum Type:
      case Question, Review, Text
