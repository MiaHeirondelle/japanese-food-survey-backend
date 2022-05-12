CREATE TYPE event_type AS ENUM ('user_login', 'respondent_data_submit', 'session_create', 'session_join', 'session_begin', 'answer_submit', 'session_pause', 'session_resume', 'session_finish');

CREATE TABLE event_log (
  id             SERIAL PRIMARY KEY,
  event_type     event_type NOT NULL,
  session_number INT REFERENCES "survey_session" NULL,
  user_id        user_id REFERENCES "user" (id) NULL,
  question_id    question_id REFERENCES "question" (id) NULL,
  timestamp_utc TIMESTAMP NOT NULL
);
