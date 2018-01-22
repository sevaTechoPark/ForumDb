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

CREATE INDEX thread_forum_created ON Thread(forumId, created);

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
  parent INT4 NOT NULL DEFAULT 0,
  parentPath int4 NOT NULL,
  FOREIGN KEY (author) REFERENCES FUser(nickname),
  FOREIGN KEY (thread) REFERENCES Thread(id),
  FOREIGN KEY (forumId) REFERENCES Forum(id)
);

-- flat
CREATE INDEX post_thread_id ON Post(thread, id);
-- tree
CREATE INDEX post_id_path ON Post(id, path);
CREATE INDEX post_thread_path ON Post(thread, path);
-- parent_tree
CREATE INDEX posts_thread_id_parentEQ0 ON Post(thread, id) WHERE parent = 0;
CREATE INDEX post_thread_parentPath ON Post(thread, parentPath);
CREATE INDEX post_id_parentPath ON Post(id, parentPath);

CREATE TABLE Vote(
  id SERIAL4 PRIMARY KEY,
  userId int4,
  threadId int4,
  voice int2 DEFAULT 0,
  FOREIGN KEY (userId) REFERENCES FUser(id),
  FOREIGN KEY (threadId) REFERENCES Thread(id)
);

CREATE INDEX vote_userId_threadId ON Vote(userId, threadId);

CREATE TABLE ForumUsers(
  userId int4,
  forumId int4,
  CONSTRAINT c_userId_forumId UNIQUE (userId, forumId),
  FOREIGN KEY (userId) REFERENCES FUser(id),
  FOREIGN KEY (forumId) REFERENCES Forum(id)
);