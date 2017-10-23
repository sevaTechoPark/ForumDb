CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

CREATE UNLOGGED TABLE FUser(
  id SERIAL4 PRIMARY KEY,
  nickname citext COLLATE "ucs_basic" NOT NULL UNIQUE,
  email citext COLLATE "ucs_basic" NOT NULL UNIQUE,
  fullname citext,
  about citext
);

CREATE INDEX nickname ON FUser(LOWER(nickname COLLATE "ucs_basic"));

CREATE UNLOGGED TABLE Forum(
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

CREATE UNLOGGED TABLE Thread(
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


CREATE UNLOGGED TABLE Post(
  id SERIAL8 PRIMARY KEY,
  forum citext,
  forumId int4 references Forum(id),
  author citext,
  userId int4 NOT NULL,
  thread int4,
  created timestamp(6) without time zone DEFAULT now() NOT NULL ,
  isEdited bool DEFAULT false NOT NULL,
  message citext NOT NULL,
  path int8[] NOT NULL,
  parent INT8 NOT NULL DEFAULT 0,
  FOREIGN KEY (userId) REFERENCES FUser(id),
  FOREIGN KEY (thread) REFERENCES Thread(id),
  FOREIGN KEY (forumId) REFERENCES Forum(id)
);

-- after pg_stat_statements
CREATE INDEX post_thread_created_id ON Post(thread, created, id);
CREATE INDEX post_thread_created_id_desc ON Post(thread, created DESC, id DESC);

CREATE INDEX post_thread_parent ON Post(thread, parent);
CREATE INDEX post_thread_path ON Post(thread, (path[1]));

CREATE INDEX post_id ON Post(id);
CREATE INDEX post_thread_path_desc ON Post(thread, path DESC);
--

CREATE INDEX post_forumId ON Post(forumId);
CREATE INDEX post_authorId ON Post(userId);
CREATE INDEX post_thread ON Post(thread);


CREATE UNLOGGED TABLE Vote(
  id SERIAL4 PRIMARY KEY,
  userId int4,
  threadId int4,
  FOREIGN KEY (userId) REFERENCES FUser(id),
  FOREIGN KEY (threadId) REFERENCES Thread(id),
  voice INT2 DEFAULT 0
);

CREATE INDEX vote_userId_threadId ON Vote(userId, threadId);

CREATE UNLOGGED TABLE ForumUsers(
  id SERIAL4 PRIMARY KEY,
  userId int4,
  forumId int4,
  FOREIGN KEY (userId) REFERENCES FUser(id),
  FOREIGN KEY (forumId) REFERENCES Forum(id),
  CONSTRAINT c_userId_forumId UNIQUE (userId, forumId)
);

CREATE OR REPLACE FUNCTION insert_ForumUsers() RETURNS TRIGGER AS '
BEGIN
  LOCK TABLE ForumUsers IN SHARE ROW EXCLUSIVE MODE;
  INSERT INTO ForumUsers(userId, forumId) VALUES (NEW.userId, NEW.forumId) ON CONFLICT DO NOTHING;
  RETURN NEW;
END;
' LANGUAGE plpgsql;

CREATE TRIGGER post_insert_trigger AFTER INSERT ON Post
FOR EACH ROW EXECUTE PROCEDURE insert_ForumUsers();


