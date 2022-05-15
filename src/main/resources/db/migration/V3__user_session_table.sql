CREATE TABLE "user_session" (
  token_hash string_hash PRIMARY KEY     NOT NULL,
  user_id    user_id                     NOT NULL REFERENCES "user" (id),
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
