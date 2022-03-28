CREATE TYPE session_element_type AS ENUM ('question', 'review', 'text');

CREATE TABLE "session_template" (
  element_order INT NOT NULL PRIMARY KEY,
  type session_element_type NOT NULL,
  question_id question_id NULL REFERENCES "question" (id),
  show_duration_seconds int NOT NULL
);