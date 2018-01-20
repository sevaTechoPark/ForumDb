package serverDb.thread;

import serverDb.post.Post;
import serverDb.user.User;
import serverDb.user.UserService;
import serverDb.vote.Vote;
import serverDb.vote.VoteRowMapper;

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

    @Autowired
    private UserService userService;

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

            for (int i = 0; i < posts.size(); i++) {
                Post post = posts.get(i);

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
        sql = "INSERT INTO Post(author, message, parent, thread, forum, forumId, created, id, path) VALUES(?,?,?,?,?,?,?,?," +
                " (SELECT path FROM Post WHERE id = ?) || ?)";

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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
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

//          UPDATE COUNT OF POST
        String sqlUpdate = "UPDATE Forum SET posts = posts + ? WHERE id = ?";
        jdbcTemplate.update(sqlUpdate, posts.size(), thread.getForumId());

        if (ids.get(ids.size() - 1) == 1500000) {
            jdbcTemplate.execute("END TRANSACTION;"
                    + "DROP INDEX IF EXISTS vote_userId_threadId;"
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

        Thread threadUpdated = getThread(slug_or_id);

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

            int rowsAffected = jdbcTemplate.update("UPDATE Thread SET message = ?, title = ? WHERE id = ?",
                    thread.getMessage(), thread.getTitle(), threadUpdated.getId());
            if (rowsAffected == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
            }

            return ResponseEntity.status(HttpStatus.OK).body(threadUpdated);

        } catch (DataIntegrityViolationException e) {

            return ResponseEntity.status(HttpStatus.OK).body(threadUpdated);
        }

    }

    public ResponseEntity voteThread(String slug_or_id, Vote vote) {

        User user = userService.getUser(vote.getNickname());
        Thread thread = getThread(slug_or_id);

        int threadId = thread.getId();
        int userId = user.getId();

        int voiceForUpdate = vote.getVoice();
        int currentVoice = voiceForUpdate;
        int existVoteId = -1;
        boolean flag = false;
        boolean flagInsert = false;

        try {   // user has voted

             Vote existVote = jdbcTemplate.queryForObject("SELECT voice, id from Vote WHERE userId = ? AND threadId = ?",
                     VoteRowMapper.INSTANCE, userId, threadId);
            if (vote.getVoice() == existVote.getVoice()) { // his voice doesn't change
                return ResponseEntity.status(HttpStatus.OK).body(thread);

            } else {    // voice changed.
                existVoteId = existVote.getId();
                voiceForUpdate = vote.getVoice() * 2;  // for example: was -1 become 1. that means we must plus 2 or -1 * (-2)
                flag = true;
            }

        } catch (EmptyResultDataAccessException e) {    // user hasn't voted

            flagInsert = true;
        }

        if (voiceForUpdate != 0) {

            thread.setVotes(thread.getVotes() + voiceForUpdate);
        }

        updateVoices(currentVoice, voiceForUpdate, existVoteId, threadId, userId, flag, flagInsert);

        return ResponseEntity.status(HttpStatus.OK).body(thread);
    }

    @Transactional
    public void updateVoices(int currentVoice, int voiceForUpdate, int existVoteId, int threadId, int userId, boolean flag, boolean flagInsert) {
        if (flagInsert) {
            jdbcTemplate.update("INSERT INTO Vote(userId, voice, threadId) VALUES(?,?,?)",
                    userId, currentVoice, threadId);
        }
        if (flag) {
            jdbcTemplate.update("UPDATE Vote SET voice = ? WHERE id = ?",
                    currentVoice, existVoteId);
        }
        if (voiceForUpdate != 0) {
            jdbcTemplate.update("UPDATE Thread SET votes = votes + ? WHERE id = ?",
                    voiceForUpdate, threadId);
        }
    }

    public ResponseEntity getPosts(String slug_or_id, Integer limit, Integer since, String sort, Boolean desc) {

        Thread thread = getThread(slug_or_id);

        int threadId = thread.getId();
        String descOrAsc = desc ? " DESC" : " ";
        String moreOrLess = desc ? " <" : " >";

        final StringBuilder sql = new StringBuilder();
        final List<Object> args = new ArrayList<>(4);

        final boolean sinceAndLimit = since != null && limit != null;

        switch (sort) {
            case "tree":
                sql.append("SELECT p1.author, p1.created, p1.forum, p1.id, p1.isEdited, p1.message, p1.parent, p1.thread FROM Post p1");
                if (sinceAndLimit) {
                    sql.append(" LEFT JOIN Post p2 on(p2.id = ?)"
                            + "WHERE p1.thread = ? AND p1.path" + moreOrLess + "p2.path"
                            + " ORDER BY p1.path" + descOrAsc + " LIMIT ?");
                    args.add(since);
                    args.add(threadId);
                    args.add(limit);
                } else if (since != null) {
                    sql.append(" LEFT JOIN Post p2 on(p2.id = ?)"
                            + "WHERE p1.thread = ? AND p1.path" + moreOrLess + "p2.path"
                            + " ORDER BY p1.path" + descOrAsc);
                    args.add(since);
                    args.add(threadId);
                } else if (limit != null) {
                    sql.append(" WHERE p1.thread = ? ORDER BY p1.path" + descOrAsc + " LIMIT ?");
                    args.add(threadId);
                    args.add(limit);
                } else {
                    sql.append(" WHERE p1.thread = ? ORDER BY p1.path" + descOrAsc);
                    args.add(threadId);
                }

                break;
            case "parent_tree":
                sql.append("SELECT p1.author, p1.created, p1.forum, p1.id, p1.isEdited, p1.message, p1.parent, p1.thread FROM Post p1");
                if (sinceAndLimit) {
                    sql.append(" JOIN (SELECT p2.id FROM Post p2 LEFT JOIN Post p3 on(p3.id = ?) WHERE p2.thread = ? AND p2.parent = 0 AND p2.id" + moreOrLess + " p3.path[1] ORDER BY p2.id" + descOrAsc + " LIMIT ?) as p"
                            + " on(p1.path[1] = p.id AND p1.thread = ?) ORDER BY p1.path" + descOrAsc);
                    args.add(since);
                    args.add(threadId);
                    args.add(limit);
                    args.add(threadId);
                } else if (since != null) {
//                    sql.append(" LEFT JOIN Post p3 on(p3.id = ?) LEFT JOIN Post p2 on(p1.path[1] = p2.id AND p1.thread = ?)"
//                            + "WHERE p2.id" + moreOrLess + " p3.path[1] AND p2.thread = ? AND p2.parent = 0"
//                            + " ORDER BY p1.path" + descOrAsc);
                    sql.append(" JOIN (SELECT p2.id FROM Post p2 LEFT JOIN Post p3 on(p3.id = ?) WHERE p2.thread = ? AND p2.parent = 0 AND p2.id" + moreOrLess + " p3.path[1]) as p"
                            + " on(p1.path[1] = p.id AND p1.thread = ?) ORDER BY p1.path" + descOrAsc);
                    args.add(since);
                    args.add(threadId);
                    args.add(threadId);
                } else if (limit != null) {
                    sql.append(" JOIN (SELECT id FROM Post WHERE thread = ? AND parent = 0 ORDER BY id" + descOrAsc + " LIMIT ?) as p"
                            + " on(p1.path[1] = p.id AND p1.thread = ?) ORDER BY p1.path" + descOrAsc);
                    args.add(threadId);
                    args.add(limit);
                    args.add(threadId);
                } else {
                    sql.append(" WHERE p1.thread = ? ORDER BY p1.path" + descOrAsc);
                }

                break;
            default:
                sql.append("SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ?");
                args.add(threadId);
                if (sinceAndLimit) {
                    sql.append(" AND id" + moreOrLess + " ?" + " ORDER BY id" + descOrAsc + " LIMIT ?");
                    args.add(since);
                    args.add(limit);
                } else if (since != null) {
                    sql.append(" AND id" + moreOrLess + " ? ORDER BY id" + descOrAsc);
                    args.add(since);
                } else if (limit != null) {
                    sql.append("ORDER BY id" + descOrAsc + " LIMIT ?");
                    args.add(limit);
                } else {
                    sql.append(" ORDER BY id" + descOrAsc);
                }
        }

        return ResponseEntity.status(HttpStatus.OK).body(
                jdbcTemplate.query(sql.toString(), args.toArray(new Object[args.size()]), serverDb.fasterMappers.PostRowMapper.INSTANCE)
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

