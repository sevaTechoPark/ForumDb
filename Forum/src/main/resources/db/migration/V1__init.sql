CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

CREATE TABLE FUser(
  id SERIAL4 PRIMARY KEY,
  nickname citext COLLATE "ucs_basic" NOT NULL UNIQUE,
  email citext COLLATE "ucs_basic" NOT NULL UNIQUE,
  fullname text,
  about text
);

-- CREATE INDEX nickname ON FUser(LOWER(nickname COLLATE "ucs_basic"));

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

-- CREATE INDEX forum_slug ON Forum(LOWER(slug COLLATE "ucs_basic"));
-- CREATE INDEX forum_user ON Forum("user");

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

-- CREATE INDEX thread_slug ON Thread(LOWER(slug COLLATE "ucs_basic"));
CREATE INDEX thread_forum ON Thread(forumId);
-- CREATE INDEX thread_author ON Thread(userId);


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

-- after pg_stat_statements
CREATE INDEX post_thread_created_id ON Post(thread, created, id );
CREATE INDEX post_thread_created_id_desc ON Post(thread, created DESC, id DESC);

CREATE INDEX post_thread_parent ON Post(thread, parent);
CREATE INDEX post_thread_path ON Post(thread, (path[1]));

-- CREATE INDEX post_id ON Post(id);
CREATE INDEX post_thread_path_desc ON Post(thread, path DESC);
--

-- CREATE INDEX post_forumId ON Post(forumId);
-- CREATE INDEX post_thread ON Post(thread);


CREATE TABLE Vote(
  id SERIAL4 PRIMARY KEY,
  userId int4,
  threadId int4,
  voice INT2 DEFAULT 0
);

CREATE INDEX vote_userId_threadId ON Vote(userId, threadId);

CREATE TABLE ForumUsers(
  id SERIAL4 PRIMARY KEY,
  userId int4,
  forumId int4,
  CONSTRAINT c_userId_forumId UNIQUE (userId, forumId)
);

CREATE TABLE PostsThread(
  postId int4 PRIMARY KEY,
  threadId int4,
  path1 int4
);

CREATE INDEX postsThread_thread_parent ON PostsThread(threadId, postId, path1);
CREATE INDEX postsThread_id_thread ON PostsThread(postId, threadId);