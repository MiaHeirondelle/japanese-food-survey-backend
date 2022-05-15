package jp.ac.tachibana.food_survey.domain.event_log

import jp.ac.tachibana.food_survey.domain.question.Question
import jp.ac.tachibana.food_survey.domain.session.Session
import jp.ac.tachibana.food_survey.domain.user.User

import java.time.Instant

case class EventLog(
  eventType: EventLog.EventType,
  sessionNumber: Option[Session.Number],
  userId: Option[User.Id],
  questionId: Option[Question.Id],
  createdAt: Instant)

object EventLog:

  enum EventType:
    case UserLogin, RespondentDataSubmit, SessionCreate, SessionJoin, SessionBegin, AnswerSubmit, SessionPause, SessionResume,
    SessionFinish
