package jp.ac.tachibana.food_survey.domain.session

import jp.ac.tachibana.food_survey.domain.question.Question

case class SessionTemplate(elements: Vector[SessionElement])

object SessionTemplate:

  extension (template: SessionTemplate)
    def elementNumberLimit: SessionElement.Number =
      SessionElement.Number(template.elements.length)

    def element(number: SessionElement.Number): Option[SessionElement] =
      if (number.value > template.elements.length)
        None
      else
        Some(template.elements(number.value))
