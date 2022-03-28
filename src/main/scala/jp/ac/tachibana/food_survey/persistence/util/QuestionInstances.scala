package jp.ac.tachibana.food_survey.persistence.util

import cats.syntax.show.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.circe.jsonb.implicits.*
import doobie.postgres.implicits.*

import jp.ac.tachibana.food_survey.domain.question.Question
import jp.ac.tachibana.food_survey.persistence.util.QuestionInstances.*

trait QuestionInstances:

  implicit val questionIdMeta: Meta[Question.Id] =
    Meta[String].timap(Question.Id(_))(_.value)

  implicit val sessionPostgresFormatQuestionTypeMeta: Meta[QuestionPostgresFormat.Type] =
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

  implicit val questionRead: Read[Question] =
    Read[(Question.Id, QuestionPostgresFormat.Type, Option[Question.Id], String, String, String)]
      .map {
        case (id, QuestionPostgresFormat.Type.Basic, None, text, scaleMinBoundCaption, scaleMaxBoundCaption) =>
          Question.Basic(
            id,
            text = text,
            Question.ScaleText(
              minBoundCaption = scaleMinBoundCaption,
              maxBoundCaption = scaleMaxBoundCaption
            )
          )

        case (
              id,
              QuestionPostgresFormat.Type.Repeated,
              Some(previousQuestionId),
              text,
              scaleMinBoundCaption,
              scaleMaxBoundCaption) =>
          Question.Repeated(
            id,
            previousQuestionId,
            text = text,
            Question.ScaleText(
              minBoundCaption = scaleMinBoundCaption,
              maxBoundCaption = scaleMaxBoundCaption
            )
          )

        case (id, _, _, _, _, _) =>
          throw new Exception(show"Incorrect question persistence data for $id")
      }

object QuestionInstances extends QuestionInstances:

  object QuestionPostgresFormat:
    enum Type:
      case Basic, Repeated
