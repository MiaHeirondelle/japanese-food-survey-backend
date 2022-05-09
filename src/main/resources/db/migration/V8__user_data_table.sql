CREATE TYPE user_sex AS ENUM ('male', 'female');


CREATE TABLE "user_data" (
  user_id user_id NOT NULL PRIMARY KEY REFERENCES "user" (id),
  sex     user_sex NULL,
  age     int NULL
);