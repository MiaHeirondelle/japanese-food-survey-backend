CREATE DOMAIN question_id AS VARCHAR(128);

CREATE TYPE question_type AS ENUM ('basic', 'repeated');

CREATE TABLE "question" (
  id                      question_id PRIMARY KEY,
  type                    question_type NOT NULL,
  previous_question_id    question_id   NULL REFERENCES "question" (id),
  text                    text          NOT NULL,
  scale_min_bound_caption text          NOT NULL,
  scale_max_bound_caption text          NOT NULL
);
