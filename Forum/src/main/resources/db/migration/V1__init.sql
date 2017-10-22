CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

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

-- after pg_stat_statements
CREATE INDEX post_thread_created ON Post(thread, created);
CREATE INDEX post_created_thread ON Post(created, thread);
CREATE INDEX post_thread_parent ON Post(thread, parent);
CREATE INDEX post_parent_thread ON Post(parent, thread);

CREATE INDEX post_path_thread ON Post(path, thread);
CREATE INDEX post_thread_path ON Post(thread, path);
--

CREATE INDEX post_forumId ON Post(forumId);
CREATE INDEX post_authorId ON Post(userId);
CREATE INDEX post_thread ON Post(thread);


CREATE TABLE Vote(
  id SERIAL4 PRIMARY KEY,
  userId int4,
  treadId int4,
  FOREIGN KEY (userId) REFERENCES FUser(id),
  FOREIGN KEY (treadId) REFERENCES Thread(id),
  voice INT2 DEFAULT 0
);

CREATE INDEX vote_userId ON Vote(userId);
CREATE INDEX vote_threadId ON Vote(treadId);

CREATE TABLE ForumUsers(
  id SERIAL4 PRIMARY KEY,
  userId int4,
  forumId int4,
  FOREIGN KEY (userId) REFERENCES FUser(id),
  FOREIGN KEY (forumId) REFERENCES Forum(id)
);

CREATE OR REPLACE FUNCTION insert_ForumUsers() RETURNS TRIGGER AS '
BEGIN
  INSERT INTO ForumUsers(userId, forumId) VALUES (NEW.userId, NEW.forumId) ON CONFLICT DO NOTHING;
  RETURN NEW;
END;
' LANGUAGE plpgsql;


CREATE TRIGGER post_insert_trigger AFTER INSERT ON Post
FOR EACH ROW EXECUTE PROCEDURE insert_ForumUsers();