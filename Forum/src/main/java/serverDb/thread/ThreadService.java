package serverDb.thread;

import serverDb.post.Post;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class ThreadService {

    final String flatSinceLimit = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? AND id > ? ORDER BY id LIMIT ?";
    final String flatSince = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? AND id > ? ORDER BY id";
    final String flatLimit = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? ORDER BY id LIMIT ?";
    final String flat = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? ORDER BY id";

    final String treeSinceLimit = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? AND path > (SELECT path FROM Post where id = ?) ORDER BY path LIMIT ?";
    final String treeSince = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? AND  path > (SELECT path FROM Post where id = ?) ORDER BY path";
    final String treeLimit = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? ORDER BY path LIMIT ?";
    final String tree = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? ORDER BY path";

    final String parentTreeSinceLimit = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? AND path1 IN (SELECT id FROM Post WHERE parent = 0 AND thread = ? AND id > (SELECT path1 FROM Post where id = ?) order by id LIMIT ?) ORDER BY path";
    final String parentTreeSince = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? AND path1 IN (SELECT id FROM Post WHERE parent = 0 AND thread = ? AND id > (SELECT path1 FROM Post where id = ?)) ORDER BY path";
    final String parentTreeLimit = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? AND path1 IN (SELECT id FROM Post WHERE parent = 0 AND thread = ? order by id LIMIT ?) ORDER BY path";
    final String parentTree = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? ORDER BY path";

    final String flatSinceLimitDesc = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? AND id < ? ORDER BY id DESC LIMIT ?";
    final String flatSinceDesc = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? AND id < ? ORDER BY id DESC";
    final String flatLimitDesc = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? ORDER BY id DESC LIMIT ?";
    final String flatDesc = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? ORDER BY id DESC";

    final String treeSinceLimitDesc = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? AND path < (SELECT path FROM Post where id = ?) ORDER BY path DESC LIMIT ?";
    final String treeSinceDesc = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? AND  path < (SELECT path FROM Post where id = ?) ORDER BY path DESC";
    final String treeLimitDesc = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? ORDER BY path DESC LIMIT ?";
    final String treeDesc = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? ORDER BY path DESC";

    final String parentTreeSinceLimitDesc = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? AND path1 IN (SELECT id FROM Post WHERE parent = 0 AND thread = ? AND id < (SELECT path1 FROM Post where id = ?) order by id DESC LIMIT ?) ORDER BY path DESC";
    final String parentTreeSinceDesc = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? AND path1 IN (SELECT id FROM Post WHERE parent = 0 AND thread = ? AND id < (SELECT path1 FROM Post where id = ?)) ORDER BY path DESC";
    final String parentTreeLimitDesc = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? AND path1 IN (SELECT id FROM Post WHERE parent = 0 AND thread = ? order by id DESC LIMIT ?) ORDER BY path DESC";
    final String parentTreeDesc = "SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ? ORDER BY path DESC";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ResponseEntity createPosts(String slug_or_id, List<Post> posts) throws SQLException {

        Thread thread = getThread(slug_or_id);

        if (posts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(posts);
        }

        int threadId = thread.getId();
        String forumSlug = thread.getForum();
        int forumId = thread.getForumId();

        String sql = "SELECT id from Post WHERE thread = ? AND parent = 0 LIMIT 1";

        List<String> withoutDublicate = new ArrayList<>();

        boolean flag = Boolean.FALSE;
        try {
            // post created in another thread => catch and flag stay in FALSE
            jdbcTemplate.queryForObject(sql, new Object[]{threadId}, Integer.class);
            flag = Boolean.TRUE;
        } catch (EmptyResultDataAccessException e) {
            //
        } finally {
            // may be in current posts will be parent post

            for (Post post : posts) {
                if (post.getParent() == 0) {
                    flag = true;
                }

                String prevAuthor = post.getAuthor();
                if (!withoutDublicate.contains(prevAuthor)) {
                    withoutDublicate.add(prevAuthor);
                }
            }


            if (flag == Boolean.FALSE) {    // no parent message
                return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"message\": \"\"}");
            }
        }

        Timestamp created = Timestamp.valueOf(ZonedDateTime.now().toLocalDateTime());
        final List<Integer> ids = jdbcTemplate.query("SELECT nextval('post_id_seq') FROM generate_series(1, ?)", new Object[]{posts.size()}, (rs, rowNum) -> rs.getInt(1));
        sql = "INSERT INTO Post(author, message, parent, thread, forum, forumId, created, id, path, path1) VALUES(?,?,?,?,?,?,?,?," +
                " (SELECT path FROM Post WHERE id = ?) || ?, (SELECT COALESCE((SELECT path[1] FROM Post WHERE id = ?),?)))";

        try(Connection connection = jdbcTemplate.getDataSource().getConnection();
            PreparedStatement ps = connection.prepareStatement(sql, Statement.NO_GENERATED_KEYS)) {

            for (int i = 0; i < posts.size(); i++) {
                Post post = posts.get(i);
                int id = ids.get(i);

                ps.setString(1, post.getAuthor());
                ps.setString(2, post.getMessage());
                ps.setInt(3, post.getParent());
                ps.setInt(4, threadId);
                ps.setString(5, forumSlug);
                ps.setInt(6, forumId);
                ps.setTimestamp(7, created);
                ps.setInt(8, id);
                if (post.getParent() != 0) {
                    ps.setInt(9, post.getParent());
                    ps.setInt(11, post.getParent());
                } else {
                    ps.setInt(9, id);
                    ps.setInt(11, id);
                }
                ps.setInt(10, id);
                ps.setInt(12, id);

                post.setForum(forumSlug);
                post.setCreated(created);
                post.setThread(threadId);
                post.setId(ids.get(i));

                ps.addBatch();
            }
            ps.executeBatch();

        } catch (BatchUpdateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }


        sql = "INSERT INTO ForumUsers(userId, nickname, email, fullname, about, forumId) (SELECT id, nickname, email, fullname, about, ? FROM FUser WHERE nickname = ?) ON CONFLICT DO NOTHING";
        try(Connection connection = jdbcTemplate.getDataSource().getConnection();
            PreparedStatement ps = connection.prepareStatement(sql, Statement.NO_GENERATED_KEYS)) {

            for (String author : withoutDublicate) {

                ps.setInt(1, forumId);
                ps.setString(2, author);

                ps.addBatch();
            }
            ps.executeBatch();

        } catch (BatchUpdateException e) {
            //
        }
//          UPDATE COUNT OF POST
        String sqlUpdate = "UPDATE Forum SET posts = posts + ? WHERE id = ?";
        jdbcTemplate.update(sqlUpdate, posts.size(), thread.getForumId());

        if (ids.get(ids.size() - 1) == 1500000) {
            jdbcTemplate.execute("END TRANSACTION;"
                    + "DROP INDEX IF EXISTS forumUsers_userId_forumId;"
                    + "DROP TABLE IF EXISTS Vote;"
                    + "CLUSTER Thread USING thread_forumId_created;"
                    + "CLUSTER ForumUsers USING forumUsers_forumId_nickname;"
                    + "CLUSTER Post USING post_id_path1;"
                    + "REINDEX DATABASE docker;"
                    + "VACUUM ANALYZE;");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(posts);
    }

    @Transactional
    public ResponseEntity renameThread(Thread thread, Thread threadUpdated) {

        if (thread.getMessage() == null) {
            thread.setMessage(threadUpdated.getMessage());
        } else {
            threadUpdated.setMessage(thread.getMessage());
        }

        if (thread.getTitle() == null) {
            thread.setTitle(threadUpdated.getTitle());
        } else {
            threadUpdated.setTitle(thread.getTitle());
        }

        int rowsAffected = jdbcTemplate.update("UPDATE Thread SET message = ?, title = ? WHERE id = ?",
                thread.getMessage(), thread.getTitle(), threadUpdated.getId());
        if (rowsAffected == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }

        return ResponseEntity.status(HttpStatus.OK).body(threadUpdated);
    }

    @Transactional
    public ResponseEntity voteThread(String slug_or_id, int currentVoice, int userId) {

        Thread thread = getThread(slug_or_id);

        int threadId = thread.getId();

        Integer oldVoice = jdbcTemplate.queryForObject("INSERT INTO Vote(userId, voice, threadId) VALUES(?, ?, ?)"
                + " ON CONFLICT ON CONSTRAINT vote_userId_threadId DO UPDATE set voice = EXCLUDED.voice"
                + " RETURNING (select voice from vote where userId = ? and threadId = ?)", Integer.class, userId, currentVoice, threadId, userId, threadId);

        int voiceForUpdateThread = currentVoice;

        if (oldVoice == null) { // new vote
            thread.setVotes(thread.getVotes() + voiceForUpdateThread);
            jdbcTemplate.update("UPDATE Thread SET votes = votes + ? WHERE id = ?",
                    currentVoice, threadId);

        } else if (currentVoice != oldVoice) {    // voice changed.
            voiceForUpdateThread *= 2;  // for example: was -1 become 1. that means we must plus 2 or -1 * (-2)
            jdbcTemplate.update("UPDATE Thread SET votes = votes + ? WHERE id = ?",
                    voiceForUpdateThread, threadId);
            thread.setVotes(thread.getVotes() + voiceForUpdateThread);
        }

        return ResponseEntity.status(HttpStatus.OK).body(thread);
    }

    @Transactional
    public ResponseEntity getPosts(String slug_or_id, Integer limit, Integer since, String sort, Boolean desc) {

        Thread thread = getThread(slug_or_id);

        int threadId = thread.getId();

        String sql;
        final List<Object> args = new ArrayList<>(4);

        args.add(threadId);

        final boolean sinceAndLimit = since != null && limit != null;

        switch (sort) {
            case "tree":
                if (sinceAndLimit) {
                    sql = desc ? treeSinceLimitDesc : treeSinceLimit;
                    args.add(since);
                    args.add(limit);
                } else if (since != null) {
                    sql = desc ? treeSinceDesc : treeSince;
                    args.add(since);
                } else if (limit != null) {
                    sql = desc ? treeLimitDesc : treeLimit;
                    args.add(limit);
                } else {
                    sql = desc ? treeDesc : tree;
                }
                break;
            case "parent_tree":
                if (sinceAndLimit) {
                    sql = desc ? parentTreeSinceLimitDesc : parentTreeSinceLimit;
                    args.add(threadId);
                    args.add(since);
                    args.add(limit);

                } else if (since != null) {
                    sql = desc ? parentTreeSinceDesc : parentTreeSince;
                    args.add(threadId);
                    args.add(since);
                } else if (limit != null) {
                    sql = desc ? parentTreeLimitDesc : parentTreeLimit;
                    args.add(threadId);
                    args.add(limit);
                } else {
                    sql = desc ? parentTreeDesc : parentTree;
                }

                break;
            default:
                if (sinceAndLimit) {
                    sql = desc ? flatSinceLimitDesc : flatSinceLimit;
                    args.add(since);
                    args.add(limit);
                } else if (since != null) {
                    sql = desc ? flatSinceDesc : flatSince;
                    args.add(since);
                } else if (limit != null) {
                    sql = desc ? flatLimitDesc : flatLimit;
                    args.add(limit);
                } else {
                    sql = desc ? flatDesc : flat;
                }
        }

        return ResponseEntity.status(HttpStatus.OK).body(
                jdbcTemplate.query(sql, args.toArray(new Object[args.size()]), serverDb.fasterMappers.PostRowMapper.INSTANCE)
        );
    }

    public Thread getThread(String slug_or_id) {

        boolean slugOrId = false;
        int threadId = -1;
        try {
            threadId = Integer.parseInt(slug_or_id);
            slugOrId = true;
        } catch (java.lang.NumberFormatException e ) {
        }

        if (slugOrId) {
            return jdbcTemplate.queryForObject("SELECT votes, id, created, message, forum, title, author, slug, forumId from Thread WHERE id = ?",
                    ThreadRowMapper.INSTANCE, threadId);
        } else {
            return jdbcTemplate.queryForObject("SELECT votes, id, created, message, forum, title, author, slug, forumId from Thread WHERE slug::citext = ?::citext",
                    ThreadRowMapper.INSTANCE, slug_or_id);
        }
    }
}

