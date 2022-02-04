CREATE TABLE user_session (
  token_hash VARCHAR(64) PRIMARY KEY NOT NULL,
  user_id    VARCHAR(32) NOT NULL
)
