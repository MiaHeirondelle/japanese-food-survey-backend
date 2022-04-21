package jp.ac.tachibana.food_survey.services.session.model

import jp.ac.tachibana.food_survey.domain.question.{Question, QuestionAnswer}
import jp.ac.tachibana.food_survey.domain.session.{Session, SessionElement}
import jp.ac.tachibana.food_survey.domain.user.User

sealed trait OutputSessionMessage

object OutputSessionMessage:
  case class UserJoined(
    user: User,
    session: Session)
      extends OutputSessionMessage

  case class SessionBegan(session: Session.InProgress) extends OutputSessionMessage

  case class BasicQuestionSelected(element: SessionElement.Question.Basic) extends OutputSessionMessage

  case class RepeatedQuestionSelected(
    element: SessionElement.Question.Repeated,
    previousAnswers: List[QuestionAnswer])
      extends OutputSessionMessage

  case class BasicQuestionReviewSelected(
    element: SessionElement.QuestionReview.Basic,
    answers: List[QuestionAnswer.Basic])
      extends OutputSessionMessage

  case class TimerTick(remainingTimeMs: Long) extends OutputSessionMessage

  case object TransitionToNextElement extends OutputSessionMessage

  case class SessionFinished(session: Session.Finished) extends OutputSessionMessage

  case object Shutdown extends OutputSessionMessage
