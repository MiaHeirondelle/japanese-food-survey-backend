package jp.ac.tachibana.food_survey.http.model.session

import jp.ac.tachibana.food_survey.domain.session.SessionAnswers
import jp.ac.tachibana.food_survey.http.model.question.QuestionAnswerFormat

object SessionAnswersFormat:

  // Maps question ids to maps of user ids to answers.
  type RawType = Map[String, Map[String, QuestionAnswerFormat]]

  def fromDomain(answers: SessionAnswers): SessionAnswersFormat.RawType =
    answers.toMap.map { case (questionId, subMap) =>
      questionId.value -> subMap.toSortedMap.map { case (userId, answer) =>
        userId.value -> QuestionAnswerFormat.fromDomain(answer)
      }
    }
