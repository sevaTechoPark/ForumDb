package serverDb.thread;

import serverDb.error.Error;
import serverDb.post.Post;
import serverDb.post.PostRowMapper;
import serverDb.user.User;
import serverDb.vote.Vote;
import serverDb.vote.VoteRowMapper;
import static serverDb.user.UserService.findUser;

import org.springframework.dao.DataIntegrityViolationException;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public ResponseEntity createPosts(String slug_or_id, List<Post> posts) throws SQLException {
//      **************************************find thread**************************************
        Thread thread = findThread(slug_or_id, jdbcTemplate);
        if (thread == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));

        }
//      **************************************find thread**************************************
        if (posts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(posts);
        }

        int threadId = thread.getId();
        String forumSlug = thread.getForum();
        int forumId = thread.getForumId();

        String sql;
        sql = "SELECT postId from PostsThread WHERE threadId = ? LIMIT 1";

        List<String> withoutDublicate = new ArrayList<>();
        List<Integer> parentPostId = new ArrayList<>();

        boolean flag = Boolean.FALSE;
        try {
            // post created in another thread => catch and flag stay in FALSE
            jdbcTemplate.queryForObject(sql, new Object[]{threadId}, Integer.class);
            flag = Boolean.TRUE;
        } catch (EmptyResultDataAccessException e) {
            //
        } finally {
            // may be in current posts will be parent post

            for (int i = 0; i < posts.size(); i++) {
                Post post = posts.get(i);

                if (post.getParent() == 0) {
                    parentPostId.add(i);
                    flag = true;
                }

                String prevAuthor = post.getAuthor();
                if (!withoutDublicate.contains(prevAuthor)) {
                    withoutDublicate.add(prevAuthor);
                }
            }


            if (flag == Boolean.FALSE) {    // no parent message
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Error.getJson(""));
            }
        }

        Timestamp created = Timestamp.valueOf(ZonedDateTime.now().toLocalDateTime());
        final List<Integer> ids = jdbcTemplate.query("SELECT nextval('post_id_seq') FROM generate_series(1, ?)", new Object[]{posts.size()}, (rs, rowNum) -> rs.getInt(1));
        sql = "INSERT INTO Post(author, message, parent, thread, forum, forumId, created, id, path) VALUES(?,?,?,?,?,?,?,?," +
                " (SELECT path FROM PathPosts WHERE postId = ?) || ?)";

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
                    ps.setLong(9, post.getParent());
                } else {
                    ps.setLong(9, id);
                }
                ps.setInt(10, id);

                post.setForum(forumSlug);
                post.setCreated(created);
                post.setThread(threadId);
                post.setId(ids.get(i));

                ps.addBatch();
            }
            ps.executeBatch();

        } catch (BatchUpdateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
        }

        sql = "INSERT INTO ForumUsers(userId, forumId) VALUES( (SELECT id FROM FUser WHERE nickname = ?), ?) " +
                "ON CONFLICT DO NOTHING";
        try(Connection connection = jdbcTemplate.getDataSource().getConnection();
            PreparedStatement ps = connection.prepareStatement(sql, Statement.NO_GENERATED_KEYS)) {

            for (int i = 0; i < withoutDublicate.size(); i++) {

                String author = withoutDublicate.get(i);

                ps.setString(1, author);
                ps.setInt(2, forumId);

                ps.addBatch();
            }
            ps.executeBatch();

        } catch (BatchUpdateException e) {
            //
        }

        sql = "INSERT INTO PostsThread(postId, threadId) VALUES(?,?)";
        try(Connection connection = jdbcTemplate.getDataSource().getConnection();
            PreparedStatement ps = connection.prepareStatement(sql, Statement.NO_GENERATED_KEYS)) {

            for (int i = 0; i < parentPostId.size(); i++) {

                int id = ids.get(parentPostId.get(i));

                ps.setInt(1, id);
                ps.setInt(2, threadId);

                ps.addBatch();
            }
            ps.executeBatch();

        } catch (BatchUpdateException e) {
            //
        }

//        final List<Array> paths = jdbcTemplate.query("SELECT path FROM Post WHERE id >= ? AND id <= ? ORDER by id",
//                new Object[]{ids.get(0), ids.get(ids.size() - 1)}, (resultSet, i) -> resultSet.getArray("path"));
        sql = "INSERT INTO PathPosts(postId, path) VALUES(?,(SELECT path FROM Post WHERE id = ?))";
        try(Connection connection = jdbcTemplate.getDataSource().getConnection();
            PreparedStatement ps = connection.prepareStatement(sql, Statement.NO_GENERATED_KEYS)) {

            for (int i = 0; i < ids.size(); i++) {

                int id = ids.get(i);
//                Array path = paths.get(i);

                ps.setInt(1, id);
                ps.setInt(2, id);

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
                    + "VACUUM ANALYZE PathPosts;"
                    + "VACUUM ANALYZE PostsThread;"
                    + "VACUUM ANALYZE ForumUsers;"
                    + "VACUUM ANALYZE Post;"
                    + "VACUUM ANALYZE Thread;"
                    + "VACUUM ANALYZE Forum;"
                    + "VACUUM ANALYZE FUser;"
                    + "REINDEX DATABASE docker;");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(posts);
    }

    public ResponseEntity renameThread(String slug_or_id, Thread thread) {

//      **************************************find thread**************************************
        Thread threadUpdated = findThread(slug_or_id, jdbcTemplate);
        if (threadUpdated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
        }
//      **************************************find thread**************************************
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

        try {
            String sql = "UPDATE Thread SET message = ?, title = ? WHERE id = ?";

            int rowsAffected = jdbcTemplate.update(sql, thread.getMessage(), thread.getTitle(), threadUpdated.getId());
            if (rowsAffected == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
            }

            return ResponseEntity.status(HttpStatus.OK).body(threadUpdated);

        } catch (DataIntegrityViolationException e) {

            return ResponseEntity.status(HttpStatus.OK).body(threadUpdated);

        }

    }

    public ResponseEntity voteThread(String slug_or_id, Vote vote) {

//      **************************************find user**************************************
        User user = findUser(vote.getNickname(), jdbcTemplate);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
        }
//      **************************************find user**************************************

//      **********************************find thread**************************************
        Thread thread = findThread(slug_or_id, jdbcTemplate);
        if (thread == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
        }
//      **************************************find thread**************************************

        int threadId = thread.getId();
        int userId = user.getId();

        int voiceForUpdate = vote.getVoice();

        try {   // user has voted

            final String sqlFindVote = "SELECT voice, id from Vote WHERE userId = ? AND threadId = ?";
             Vote existVote = jdbcTemplate.queryForObject(
                    sqlFindVote, new Object[]{userId, threadId}, VoteRowMapper.INSTANCE);
            final int voice = existVote.getVoice();
            if (vote.getVoice() == voice) { // his voice doesn't change
                return ResponseEntity.status(HttpStatus.OK).body(thread);

            } else {    // voice changed.

                final String sqlUpdateVote = "UPDATE Vote SET voice = ? WHERE id = ?";
                jdbcTemplate.update(sqlUpdateVote, voiceForUpdate, existVote.getId());
                voiceForUpdate = vote.getVoice() * 2;  // for example: was -1 become 1. that means we must plus 2 or -1 * (-2)
            }

        } catch (EmptyResultDataAccessException e) {    // user hasn't voted

            final String sqlInsertVote = "INSERT INTO Vote(userId, voice, threadId) VALUES(?,?,?)";

            jdbcTemplate.update(sqlInsertVote, userId, vote.getVoice(), threadId);

        }

        if (voiceForUpdate != 0) {
            int threadVoices = thread.getVotes() + voiceForUpdate;

            final String sql = "UPDATE Thread SET votes = ? WHERE id = ?";
            jdbcTemplate.update(sql, threadVoices, threadId); // update threads

            thread.setVotes(threadVoices);
        }

        return ResponseEntity.status(HttpStatus.OK).body(thread);
    }

    public ResponseEntity getThread(String slug_or_id) {

        Thread thread = findThread(slug_or_id, jdbcTemplate);
        if (thread == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
        }

        return ResponseEntity.status(HttpStatus.OK).body(thread);
    }

    public ResponseEntity getPosts(String slug_or_id, Integer limit, Integer since, String sort, Boolean desc) {

//      **************************************find thread**************************************
        Thread thread = findThread(slug_or_id, jdbcTemplate);
        if (thread == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));

        }
//      **************************************find thread**************************************

        int threadId = thread.getId();
        String descOrAsc = desc ? " DESC" : " ASC";
        String moreOrLess = desc ? " <" : " >";

        final StringBuilder sql = new StringBuilder();
        final List<Object> args = new ArrayList<>();

        switch (sort) {
            case "tree":
                sql.append("SELECT author, created, forum, id, isEdited, message, parent, thread, forumId from Post WHERE thread = ?");
                args.add(threadId);

                if (since != null) {
                    sql.append(" AND path");
                    sql.append(moreOrLess);

                    sql.append(" (SELECT path FROM PathPosts where postId = ?)");
                    args.add(since);

                }

                sql.append(" ORDER BY path");
                sql.append(descOrAsc);

                if (limit != null) {
                    sql.append(" LIMIT ?");
                    args.add(limit);
                }
                break;
            case "parent_tree":
                sql.append("SELECT author, created, forum, id, isEdited, message, parent, thread, forumId from Post WHERE thread = ?");
                args.add(threadId);
                if (since != null) {
                    sql.append(" AND path[1]");
                }

                if (limit != null) {
                    if (since == null) {
                        sql.append(" AND path[1]");
                    }
                    sql.append(" IN (SELECT postId as id FROM PostsThread WHERE threadId = ?");
                    args.add(threadId);
                    if (since != null) {
                        sql.append(" AND postId");
                    }
                }

                if (since != null) {
                    sql.append(moreOrLess);

                    sql.append(" (SELECT path[1] FROM PathPosts where postId = ?)");
                    args.add(since);
                }

                if (limit != null) {
                    sql.append(" order by id " + descOrAsc +" LIMIT ?)");
                    args.add(limit);
                }

                sql.append(" ORDER BY path");
                sql.append(descOrAsc);

                break;
            default:
                sql.append("SELECT author, created, forum, id, isEdited, message, parent, thread, forumId from Post WHERE thread = ?");
                args.add(threadId);
                if (since != null) {
                    sql.append(" AND id");
                    sql.append(moreOrLess);
                    sql.append(" ?");
                    args.add(since);
                }

                sql.append(" ORDER BY id");
                sql.append(descOrAsc);

                if (limit != null) {
                    sql.append(" LIMIT ?");
                    args.add(limit);
                }

        }

        return ResponseEntity.status(HttpStatus.OK).body(
                jdbcTemplate.query(sql.toString(), args.toArray(new Object[args.size()]), PostRowMapper.INSTANCE)
        );
    }

    public static Thread findThread(String slug_or_id, JdbcTemplate jdbcTemplate) {

        boolean slugOrId = false;
        int threadId = -1;
        try {
            threadId = Integer.parseInt(slug_or_id);
            slugOrId = true;
        } catch (java.lang.NumberFormatException e ) {
            //
        }

        try {

            String sql;
            if (slugOrId) {
                sql = "SELECT * from Thread WHERE id = ?";
                return jdbcTemplate.queryForObject(
                        sql, new Object[] {threadId}, ThreadRowMapper.INSTANCE);
            } else {
                sql = "SELECT * from Thread WHERE slug::citext = ?::citext";
                return jdbcTemplate.queryForObject(
                        sql, new Object[] {slug_or_id}, ThreadRowMapper.INSTANCE);
            }

        } catch (Exception e) {

            return null;
        }
    }

}

