CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;

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
  id SERIAL4 PRIMARY KEY,
  forum citext references Forum(slug),
  author citext references FUser(nickname),
  slug citext UNIQUE,
  created timestamptz(6) DEFAULT now() NOT NULL,
  message citext NOT NULL,
  title citext NOT NULL,
  votes INT4 DEFAULT 0
);

CREATE TABLE Post(
  id SERIAL8 PRIMARY KEY,
  forum citext references Forum(slug),
  author citext references FUser(nickname),
  thread int4 references Thread(id),
  created timestamptz(6) DEFAULT now() NOT NULL,
  isEdited bool DEFAULT false NOT NULL,
  message citext NOT NULL,
  parent INT8 NOT NULL DEFAULT 0
);

CREATE TABLE Vote(
  id SERIAL4 PRIMARY KEY,
  nickname citext references FUser(nickname),
  thread citext references Thread(slug),
  voice INT2 NOT NULL
);
