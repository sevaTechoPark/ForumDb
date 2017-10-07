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
  slug citext PRIMARY KEY,
  forum citext references Forum(slug),
  author citext references FUser(nickname),
  created timestamptz(6) DEFAULT now() NOT NULL,
  message citext NOT NULL,
  title citext NOT NULL,
  votes int DEFAULT 0
);

CREATE TABLE Post(
  id SERIAL PRIMARY KEY,
  forum citext references Forum(slug),
  author citext references FUser(nickname),
  thread citext references Thread(slug),
  created timestamptz(6) DEFAULT now() NOT NULL,
  isEdited bool DEFAULT false NOT NULL,
  message citext NOT NULL,
  parent BIGINT NOT NULL DEFAULT 0
);

-- drop table forum, fuser, thread, post, schema_version;



