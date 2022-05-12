package jp.ac.tachibana.food_survey.persistence.formats

import doobie.Meta
import doobie.postgres.implicits.pgEnumStringOpt
import cats.syntax.option.*
import jp.ac.tachibana.food_survey.domain.event_log.EventLog
import jp.ac.tachibana.food_survey.persistence.formats.QuestionInstances.QuestionPostgresFormat

trait EventLogInstances:

  implicit val eventLogTypeMeta: Meta[EventLog.EventType] =
    pgEnumStringOpt(
      "event_type",
      {
        case "user_login"             => EventLog.EventType.UserLogin.some
        case "respondent_data_submit" => EventLog.EventType.RespondentDataSubmit.some
        case "session_create"         => EventLog.EventType.SessionCreate.some
        case "session_join"           => EventLog.EventType.SessionJoin.some
        case "session_begin"          => EventLog.EventType.SessionBegin.some
        case "answer_submit"          => EventLog.EventType.AnswerSubmit.some
        case "session_pause"          => EventLog.EventType.SessionPause.some
        case "session_resume"         => EventLog.EventType.SessionResume.some
        case "session_finish"         => EventLog.EventType.SessionFinish.some
        case _                        => none
      },
      {
        case EventLog.EventType.UserLogin            => "user_login"
        case EventLog.EventType.RespondentDataSubmit => "respondent_data_submit"
        case EventLog.EventType.SessionCreate        => "session_create"
        case EventLog.EventType.SessionJoin          => "session_join"
        case EventLog.EventType.SessionBegin         => "session_begin"
        case EventLog.EventType.AnswerSubmit         => "answer_submit"
        case EventLog.EventType.SessionPause         => "session_pause"
        case EventLog.EventType.SessionResume        => "session_resume"
        case EventLog.EventType.SessionFinish        => "session_finish"
      }
    )

object EventLogInstances extends EventLogInstances
