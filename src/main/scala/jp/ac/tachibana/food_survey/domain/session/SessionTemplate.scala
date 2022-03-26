package jp.ac.tachibana.food_survey.domain.session

import cats.data.NonEmptyVector

import jp.ac.tachibana.food_survey.domain.question.Question

case class SessionTemplate(elements: NonEmptyVector[SessionElement])

object SessionTemplate:

  extension (template: SessionTemplate)
    def elementNumberLimit: SessionElement.Number =
      SessionElement.Number(template.elements.length)

    def element(number: SessionElement.Number): Option[SessionElement] =
      template.elements.get(number.value)
