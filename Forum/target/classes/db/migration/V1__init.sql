CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;

CREATE TABLE FUser(
  id SERIAL4 PRIMARY KEY,
  nickname citext COLLATE "ucs_basic" NOT NULL UNIQUE,
  email citext COLLATE "ucs_basic" NOT NULL UNIQUE,
  fullname citext,
  about citext
);

CREATE INDEX nickname ON FUser(LOWER(nickname COLLATE "ucs_basic"));

CREATE TABLE Forum(
  id SERIAL4 PRIMARY KEY,
  slug citext COLLATE "ucs_basic" NOT NULL UNIQUE,
  "user" citext,
  userId int4,
  posts INT8 DEFAULT 0,
  threads INT4 DEFAULT 0,
  title citext NOT NULL,
  FOREIGN KEY (userId) REFERENCES FUser(id)
);

CREATE INDEX forum_slug ON Forum(LOWER(slug COLLATE "ucs_basic"));
CREATE INDEX forum_user ON Forum("user");

CREATE TABLE Thread(
  id SERIAL4 PRIMARY KEY,
  forum citext,
  forumId int4,
  author citext,
  userId int4,
  slug citext COLLATE "ucs_basic" UNIQUE,
  created timestamptz(6) DEFAULT now() NOT NULL,
  message citext NOT NULL,
  title citext NOT NULL,
  votes INT4 DEFAULT 0,
  FOREIGN KEY (userId) REFERENCES FUser(id),
  FOREIGN KEY (forumId) REFERENCES Forum(id)

);

CREATE INDEX thread_slug ON Thread(LOWER(slug COLLATE "ucs_basic"));
CREATE INDEX thread_forum ON Thread(forumId);
CREATE INDEX thread_author ON Thread(userId);


CREATE TABLE Post(
  id SERIAL8 PRIMARY KEY,
  forum citext,
  forumId int4 references Forum(id),
  author citext,
  userId int4 NOT NULL,
  thread int4,
  created timestamptz(6) DEFAULT now() NOT NULL,
  isEdited bool DEFAULT false NOT NULL,
  message citext NOT NULL,
  path int8[] NOT NULL,
  parent INT8 NOT NULL DEFAULT 0,
  FOREIGN KEY (userId) REFERENCES FUser(id),
  FOREIGN KEY (thread) REFERENCES Thread(id),
  FOREIGN KEY (forumId) REFERENCES Forum(id)
);

CREATE INDEX post_forum ON Post(forumId);
CREATE INDEX post_author ON Post(userId);
CREATE INDEX post_thread ON Post(thread);
CREATE INDEX post_path ON Post(path);
CREATE INDEX post_level ON Post(array_length(path, 1));

CREATE TABLE Vote(
  id SERIAL4 PRIMARY KEY,
  userId int4 references FUser(id),
  treadId int4 references Thread(id),
  voice INT2 DEFAULT 0
);

CREATE INDEX vote_userId ON Vote(userId);
CREATE INDEX vote_threadId ON Vote(treadId);