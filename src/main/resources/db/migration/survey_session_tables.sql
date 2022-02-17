CREATE TYPE session_status AS ENUM ('awaiting_users', 'can_begin', 'in_progress', 'finished');

CREATE TABLE "survey_session"(
  -- todo: fix
  session_number SERIAL PRIMARY KEY,
  admin_id user_id NOT NULL REFERENCES "user" (id),
  status session_status NOT NULL,
  state jsonb NOT NULL
);

CREATE TABLE "survey_session_participant"(
  session_number INT REFERENCES "survey_session" (session_number),
  user_id user_id REFERENCES "user" (id),
  PRIMARY KEY (session_number, user_id)
);