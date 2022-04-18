CREATE TYPE session_element_type AS ENUM ('question', 'question_review', 'text');

CREATE TABLE "session_template_element" (
  element_number INT NOT NULL PRIMARY KEY,
  type session_element_type NOT NULL,
  question_id question_id NULL REFERENCES "question" (id),
  show_duration_seconds int NOT NULL
);