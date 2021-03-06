CREATE DOMAIN user_id AS VARCHAR(36);

CREATE TYPE user_role AS ENUM ('respondent', 'admin');

CREATE TABLE "user" (
  id   user_id PRIMARY KEY,
  name VARCHAR(100),
  role user_role
);

INSERT INTO "user"
  (id, name, role)
VALUES ('root', 'root', 'admin');
