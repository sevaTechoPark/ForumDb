CREATE EXTENSION IF NOT EXISTS citext ;

CREATE TABLE FUser(
  nickname citext NOT NULL PRIMARY KEY,
  email citext NOT NULL UNIQUE,
  fullname citext,
  about citext
);

CREATE TABLE Forum(
  slug citext NOT NULL PRIMARY KEY,
  "user" citext references FUser(nickname),
  posts bigint DEFAULT 0,
  threads int DEFAULT 0,
  title citext NOT NULL
);

CREATE TABLE Thread(
  id SERIAL PRIMARY KEY,
  forum citext references Forum(slug),
  author citext references FUser(nickname),
  created timestamptz(6) DEFAULT now() NOT NULL,
  message citext NOT NULL,
  slug citext NOT NULL UNIQUE,
  title citext NOT NULL,
  votes int DEFAULT 0
);

CREATE TABLE Post(
  id SERIAL PRIMARY KEY,
  forum citext references Forum(slug),
  author citext references FUser(nickname),
  created timestamptz(6) DEFAULT now() NOT NULL,
  isEdited bool DEFAULT false NOT NULL,
  message citext NOT NULL,
  parent BIGINT NOT NULL DEFAULT 0,
  thread int references Thread(id)
);



