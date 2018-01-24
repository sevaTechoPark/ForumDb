CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;
-- CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

CREATE TABLE FUser(
  id SERIAL4 PRIMARY KEY,
  nickname citext COLLATE "ucs_basic" NOT NULL UNIQUE,
  email citext COLLATE "ucs_basic" NOT NULL UNIQUE,
  fullname text,
  about text
);

CREATE INDEX fuser_covering ON FUser(nickname, id, email, fullname, about);

CREATE TABLE Forum(
  id SERIAL4 PRIMARY KEY,
  slug citext COLLATE "ucs_basic" NOT NULL UNIQUE,
  "user" text,
  userId int4,
  posts int4 DEFAULT 0,
  threads int4 DEFAULT 0,
  title text NOT NULL,
  FOREIGN KEY (userId) REFERENCES FUser(id)
);

CREATE TABLE Thread(
  id SERIAL4 PRIMARY KEY,
  forum text,
  forumId int4,
  author text,
  userId int4,
  slug citext COLLATE "ucs_basic" UNIQUE,
  created timestamp(6) with time zone DEFAULT now() NOT NULL,
  message text NOT NULL,
  title text NOT NULL,
  votes INT4 DEFAULT 0,
  FOREIGN KEY (userId) REFERENCES FUser(id),
  FOREIGN KEY (forumId) REFERENCES Forum(id)
);

CREATE INDEX thread_forumId_created ON Thread(forumId, created);

CREATE TABLE Post(
  id SERIAL4 PRIMARY KEY,
  forum text,
  forumId int4,
  author citext,
  thread int4,
  created timestamp(6) with time zone DEFAULT now() NOT NULL,
  isEdited bool DEFAULT false NOT NULL,
  message text NOT NULL,
  path int4[] NOT NULL,
  parent int4 NOT NULL DEFAULT 0,
  path1 int4 NOT NULL,
  FOREIGN KEY (author) REFERENCES FUser(nickname),
  FOREIGN KEY (thread) REFERENCES Thread(id),
  FOREIGN KEY (forumId) REFERENCES Forum(id)
);

-- flat
CREATE INDEX post_id_path ON Post(id, path); -- 2984
-- tree
CREATE INDEX post_thread_id ON Post(thread, id); -- 1907
CREATE INDEX post_thread_path ON Post(thread, path); -- 2387
-- parent_tree
CREATE INDEX post_thread_path1 ON Post(thread, path1); -- 2063
CREATE INDEX post_id_path1 ON Post(id, path1); -- 2980
CREATE INDEX posts_thread_id ON Post(thread, id) WHERE parent = 0;

CREATE TABLE Vote(
  id SERIAL4 PRIMARY KEY,
  userId int4,
  threadId int4,
  voice int2 DEFAULT 0,
  CONSTRAINT vote_userId_threadId UNIQUE (userId, threadId)
);

CREATE TABLE ForumUsers(
  userId int4,
  nickname citext COLLATE "ucs_basic",
  email text,
  fullname text,
  about text,
  forumId int4
);

CREATE UNIQUE INDEX forumUsers_userId_forumId ON ForumUsers(userId, forumId);
CREATE INDEX forumUsers_forumId_nickname ON ForumUsers(forumId, nickname);
