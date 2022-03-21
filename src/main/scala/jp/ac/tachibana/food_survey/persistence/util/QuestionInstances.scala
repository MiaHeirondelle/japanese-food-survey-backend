package jp.ac.tachibana.food_survey.persistence.util

import doobie.Read

import jp.ac.tachibana.food_survey.domain.question.QuestionAnswer

trait QuestionInstances:

  implicit val questionAnswerRead: Read[QuestionAnswer] =
    ???
