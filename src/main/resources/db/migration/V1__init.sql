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
  posts INT8 DEFAULT 0,
  threads INT4 DEFAULT 0,
  title citext NOT NULL
);

CREATE TABLE Thread(
  slug citext PRIMARY KEY,
  forum citext references Forum(slug),
  author citext references FUser(nickname),
  created timestamptz(6) DEFAULT now() NOT NULL,
  message citext NOT NULL,
  title citext NOT NULL,
  votes INT4 DEFAULT 0,
  isParent bool DEFAULT false NOT NULL
);

CREATE TABLE Post(
  id SERIAL PRIMARY KEY,
  forum citext references Forum(slug),
  author citext references FUser(nickname),
  thread citext references Thread(slug),
  created timestamptz(6) DEFAULT now() NOT NULL,
  isEdited bool DEFAULT false NOT NULL,
  message citext NOT NULL,
  parent INT8 NOT NULL DEFAULT 0
);

CREATE TABLE Vote(
  id SERIAL PRIMARY KEY,
  nickname citext references FUser(nickname),
  thread citext references Thread(slug),
  voice INT2 NOT NULL
);
