package jp.ac.tachibana.food_survey.http.model.session

import cats.data.NonEmptyList
import io.circe.Decoder

case class CreateSessionRequest(
  respondents: NonEmptyList[String])
    derives Decoder
