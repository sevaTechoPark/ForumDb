CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;
-- CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

CREATE TABLE FUser(
  id SERIAL4 PRIMARY KEY,
  nickname citext COLLATE "ucs_basic" NOT NULL UNIQUE,
  email citext COLLATE "ucs_basic" NOT NULL UNIQUE,
  fullname text,
  about text
);

CREATE TABLE Forum(
  id SERIAL4 PRIMARY KEY,
  slug citext COLLATE "ucs_basic" NOT NULL UNIQUE,
  "user" citext,
  userId int4,
  posts INT8 DEFAULT 0,
  threads INT4 DEFAULT 0,
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

CREATE INDEX thread_forum ON Thread(forumId);

CREATE TABLE Post(
  id SERIAL4 PRIMARY KEY,
  forum text,
  forumId int4 references Forum(id),
  author citext,
  thread int4,
  created timestamp(6) with time zone DEFAULT now() NOT NULL,
  isEdited bool DEFAULT false NOT NULL,
  message text NOT NULL,
  path int4[] NOT NULL,
  parent INT4 NOT NULL DEFAULT 0,
  FOREIGN KEY (author) REFERENCES FUser(nickname),
  FOREIGN KEY (thread) REFERENCES Thread(id),
  FOREIGN KEY (forumId) REFERENCES Forum(id)
);

CREATE INDEX post_thread_created_id ON Post(thread, created, id);
CREATE INDEX post_thread_path1 ON Post(thread, (path[1]));
CREATE INDEX post_thread_path ON Post(thread, path);
CREATE INDEX post_id_path1 ON Post(id, (path[1]));

CREATE TABLE Vote(
  userId int4,
  threadId int4,
  voice INT2 DEFAULT 0,
  FOREIGN KEY (userId) REFERENCES FUser(id),
  FOREIGN KEY (threadId) REFERENCES Thread(id)
);


CREATE TABLE ForumUsers(
  userId int4,
  forumId int4,
  CONSTRAINT c_userId_forumId UNIQUE (userId, forumId),
  FOREIGN KEY (userId) REFERENCES FUser(id),
  FOREIGN KEY (forumId) REFERENCES Forum(id)
);

CREATE TABLE PostsThread(
  postId int4,
  threadId int4,
  FOREIGN KEY (postId) REFERENCES Post(id),
  FOREIGN KEY (threadId) REFERENCES Thread(id)
);

CREATE INDEX postsThread_thread_parent ON PostsThread(threadId, postId);
