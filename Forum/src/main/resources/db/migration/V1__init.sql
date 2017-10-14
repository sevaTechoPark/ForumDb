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

CREATE INDEX forum_user ON Forum ("user");

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

CREATE INDEX thread_forum ON Thread (forum);
CREATE INDEX thread_author ON Thread (author);



CREATE TABLE Post(
  id SERIAL8 PRIMARY KEY,
  forum citext references Forum(slug),
  author citext references FUser(nickname),
  thread int4 references Thread(id),
  created timestamptz(6) DEFAULT now() NOT NULL,
  isEdited bool DEFAULT false NOT NULL,
  message citext NOT NULL,
  path int8[] NOT NULL,
  parent INT8 NOT NULL DEFAULT 0
);

CREATE INDEX post_forum ON Post (forum);
CREATE INDEX post_author ON Post (author);
CREATE INDEX post_thread ON Post (thread);
CREATE INDEX post_path ON Post (path);
CREATE INDEX post_level ON Post (array_length(path, 1));

CREATE TABLE Vote(
id SERIAL4 PRIMARY KEY,
nickname citext references FUser(nickname),
thread citext references Thread(slug),
voice INT2 NOT NULL
);
