package jp.ac.tachibana.food_survey.persistence.formats

import cats.syntax.show.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.circe.jsonb.implicits.*
import doobie.postgres.implicits.*
import cats.effect.Sync
import cats.syntax.option.*
import java.util.UUID

import jp.ac.tachibana.food_survey.domain.question.{Question, QuestionAnswer}
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User
import jp.ac.tachibana.food_survey.persistence.formats.QuestionInstances.*

trait QuestionInstances:

  implicit val questionIdMeta: Meta[Question.Id] =
    Meta[String].timap(Question.Id(_))(_.value)

  implicit val answerIdMeta: Meta[QuestionInstances.AnswerId] =
    Meta[String].timap(QuestionInstances.AnswerId(_))(_.value)

  implicit val questionPostgresFormatTypeMeta: Meta[QuestionPostgresFormat.Type] =
    pgEnumStringOpt(
      "question_type",
      {
        case "basic"    => Some(QuestionPostgresFormat.Type.Basic)
        case "repeated" => Some(QuestionPostgresFormat.Type.Repeated)
        case _          => None
      },
      {
        case QuestionPostgresFormat.Type.Basic    => "basic"
        case QuestionPostgresFormat.Type.Repeated => "repeated"
      }
    )

  implicit val answerPostgresFormatTypeMeta: Meta[AnswerPostgresFormat.Type] =
    pgEnumStringOpt(
      "answer_type",
      {
        case "basic"    => Some(AnswerPostgresFormat.Type.Basic)
        case "repeated" => Some(AnswerPostgresFormat.Type.Repeated)
        case _          => None
      },
      {
        case AnswerPostgresFormat.Type.Basic    => "basic"
        case AnswerPostgresFormat.Type.Repeated => "repeated"
      }
    )

  implicit val questionRead: Read[Question] =
    Read[(Question.Id, QuestionPostgresFormat.Type, Option[Question.Id], String, String, String)]
      .map {
        case (id, QuestionPostgresFormat.Type.Basic, None, text, scaleMin, scaleMax) =>
          Question.Basic(
            id,
            text = text,
            Question.ScaleText(
              minBoundCaption = scaleMin,
              maxBoundCaption = scaleMax
            )
          )

        case (id, QuestionPostgresFormat.Type.Repeated, Some(previousQuestionId), text, scaleMin, scaleMax) =>
          Question.Repeated(
            id,
            previousQuestionId,
            text = text,
            Question.ScaleText(
              minBoundCaption = scaleMin,
              maxBoundCaption = scaleMax
            )
          )

        case (id, _, _, _, _, _) =>
          throw new Exception(show"Incorrect question persistence data for $id")
      }

private[persistence] object QuestionInstances extends QuestionInstances:

  object QuestionPostgresFormat:
    enum Type:
      case Basic, Repeated

  opaque type AnswerId = String

  object AnswerId:
    def generate[F[_]: Sync]: F[AnswerId] =
      Sync[F].delay(UUID.randomUUID().toString)

    def apply(value: String): QuestionInstances.AnswerId =
      value

    extension (id: QuestionInstances.AnswerId) def value: String = id

  case class AnswerPostgresFormat(
    id: QuestionInstances.AnswerId,
    `type`: AnswerPostgresFormat.Type,
    sessionNumber: Session.Number,
    respondentId: User.Id,
    questionId: Question.Id,
    previousQuestionId: Option[Question.Id])

  object AnswerPostgresFormat:
    enum Type:
      case Basic, Repeated

    def fromDomain(
      id: QuestionInstances.AnswerId,
      answer: QuestionAnswer): QuestionInstances.AnswerPostgresFormat =
      QuestionInstances.AnswerPostgresFormat(
        id,
        typeFromDomain(answer),
        answer.sessionNumber,
        answer.respondentId,
        answer.questionId,
        previousQuestionIdFromDomain(answer)
      )

    private def typeFromDomain(answer: QuestionAnswer): QuestionInstances.AnswerPostgresFormat.Type =
      answer match {
        case _: QuestionAnswer.Basic =>
          QuestionInstances.AnswerPostgresFormat.Type.Basic
        case _: QuestionAnswer.Repeated =>
          QuestionInstances.AnswerPostgresFormat.Type.Repeated
      }

    private def previousQuestionIdFromDomain(answer: QuestionAnswer): Option[Question.Id] =
      answer match {
        case _: QuestionAnswer.Basic =>
          none
        case q: QuestionAnswer.Repeated =>
          q.previousQuestionId.some
      }
