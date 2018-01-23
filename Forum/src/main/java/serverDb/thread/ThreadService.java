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
                    ps.setLong(9, post.getParent());
                    ps.setLong(11, post.getParent());
                } else {
                    ps.setLong(9, id);
                    ps.setLong(11, id);
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
                    + "DROP INDEX IF EXISTS vote_userId_threadId;"
                    + "DROP INDEX IF EXISTS forumUsers_userId_forumId;"
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
        String descOrAsc = desc ? " DESC" : " ASC";
        String moreOrLess = desc ? " <" : " >";

        final StringBuilder sql = new StringBuilder("SELECT author, created, forum, id, isEdited, message, parent, thread from Post WHERE thread = ?");
        final List<Object> args = new ArrayList<>(4);

        args.add(threadId);

        final boolean sinceAndLimit = since != null && limit != null;

        switch (sort) {
            case "tree":
                if (sinceAndLimit) {
                    sql.append(" AND path" + moreOrLess + " (SELECT path FROM Post where id = ?)"
                            + " ORDER BY path" + descOrAsc + " LIMIT ?");
                    args.add(since);
                    args.add(limit);
                } else if (since != null) {
                    sql.append(" AND path" + moreOrLess + " (SELECT path FROM Post where id = ?)"
                            + " ORDER BY path" + descOrAsc);
                    args.add(since);
                } else if (limit != null) {
                    sql.append("ORDER BY path" + descOrAsc + " LIMIT ?");
                    args.add(limit);
                } else {
                    sql.append(" ORDER BY path" + descOrAsc);
                }

                break;
            case "parent_tree":
                if (sinceAndLimit) {
                    sql.append(" AND path1 IN (SELECT id FROM Post WHERE parent = 0 AND thread = ?"
                            + " AND id" + moreOrLess + "(SELECT path1 FROM Post where id = ?)"
                            + "order by id" + descOrAsc + " LIMIT ?)  ORDER BY path" + descOrAsc);
                    args.add(threadId);
                    args.add(since);
                    args.add(limit);

                } else if (since != null) {
                    sql.append(" AND path1 IN (SELECT id FROM Post WHERE parent = 0 AND thread = ?"
                            + " AND id" + moreOrLess + "(SELECT path1 FROM Post where id = ?)"
                            + "order by id" + descOrAsc + ")  ORDER BY path" + descOrAsc);
                    args.add(threadId);
                    args.add(since);
                } else if (limit != null) {
                    sql.append(" AND path1 IN (SELECT id FROM Post WHERE parent = 0 AND thread = ?"
                            + "order by id " + descOrAsc + " LIMIT ?)  ORDER BY path" + descOrAsc);
                    args.add(threadId);
                    args.add(limit);
                } else {
                    sql.append(" ORDER BY path" + descOrAsc);
                }

                break;
            default:
                if (sinceAndLimit) {
                    sql.append(" AND id" + moreOrLess + " ?"
                            + " ORDER BY id" + descOrAsc
                            + " LIMIT ?");
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

