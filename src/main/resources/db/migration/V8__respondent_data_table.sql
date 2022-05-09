CREATE TYPE respondent_sex AS ENUM ('male', 'female');


CREATE TABLE "respondent_data" (
  user_id user_id NOT NULL PRIMARY KEY REFERENCES "user" (id),
  sex     respondent_sex NULL,
  age     int NULL
);