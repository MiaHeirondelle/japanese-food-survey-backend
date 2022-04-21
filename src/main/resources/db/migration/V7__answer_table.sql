CREATE DOMAIN answer_id AS VARCHAR(36);

CREATE TYPE answer_type AS ENUM ('basic', 'repeated');

CREATE TABLE "answer"
(
  id                   answer_id   NOT NULL PRIMARY KEY,
  type                 answer_type NOT NUll,
  session_number       int         NOT NULL REFERENCES "survey_session" (session_number),
  respondent_id        user_id     NOT NULL REFERENCES "user" (id),
  question_id          question_id NOT NULL REFERENCES "question" (id),
  previous_question_id question_id NULL REFERENCES "question" (id),
  scale_value          INT NULL,
  comment              TEXT NULL,
  UNIQUE (session_number, respondent_id, question_id)
);