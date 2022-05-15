CREATE DOMAIN string_hash AS VARCHAR(256);

CREATE TABLE "user_credentials" (
  user_id       varchar(36) PRIMARY KEY REFERENCES "user" (id),
  login         VARCHAR(100) UNIQUE,
  password_hash string_hash,
  password_salt bytea
);

INSERT INTO "user_credentials"
  (user_id, login, password_hash, password_salt)
  VALUES ('root', 'admin', '00000000000000000000000000000000000000000000000077e0045ed7d8552b', E'\\xCEC76FA6A9B1209534FBF5E3C9E52A79');
