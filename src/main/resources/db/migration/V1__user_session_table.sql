CREATE TABLE user_session (
  user_id    VARCHAR(32) PRIMARY KEY NOT NULL,
  token_hash VARCHAR(64) UNIQUE NOT NULL
)
