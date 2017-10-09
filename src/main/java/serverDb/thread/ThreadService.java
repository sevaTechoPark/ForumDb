package serverDb.thread;

import serverDb.error.Error;
import serverDb.post.Post;
import serverDb.post.PostRowMapper;
import serverDb.vote.Vote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.dao.EmptyResultDataAccessException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.time.ZonedDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@Service
public class ThreadService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ResponseEntity createPosts(String slug, int id, List<Post> posts) {

        boolean flag;
        String forumSlug;
        String threadSlug;

        try {

            final String sqlGetThread = "SELECT * from Thread WHERE slug = ? OR id = ?";
            Thread thread = (Thread) jdbcTemplate.queryForObject(
                    sqlGetThread, new Object[] { slug, id }, new ThreadRowMapper());

            threadSlug = thread.getSlug();
            forumSlug = thread.getForum();
            flag = thread.getIsParent();

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find thread: " + slug),
                    HttpStatus.NOT_FOUND);
        }

        if(flag == false) { // may be in current posts will be parent post
            ListIterator<Post> listIter = posts.listIterator();

            while(listIter.hasNext()){

                if (listIter.next().getParent() == 0) {
                    flag = true;
                    final String sqlUpdateThread = "UPDATE Thread SET isParent = TRUE WHERE slug = ?";
                    jdbcTemplate.update(sqlUpdateThread, new Object[] { threadSlug });
                    break;
                }
            }
        }

        if (flag == false) {    // no parent message
            return new ResponseEntity(Error.getJson("Missed parent post!"),
                    HttpStatus.CONFLICT);
        }

        Timestamp created = Timestamp.valueOf(ZonedDateTime.now().toLocalDateTime());

        final String sql = "INSERT INTO Post(author, message, parent, thread, forum, created) VALUES(?,?,?,?,?,?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {

                Post post = posts.get(i);

                ps.setString(1, post.getAuthor());
                ps.setString(2, post.getMessage());
                ps.setLong(3, post.getParent());
                ps.setString(4, threadSlug);
                ps.setString(5, forumSlug);
                ps.setTimestamp(6, created);

                post.setForum(forumSlug);
                post.setCreated(created.toString());
                post.setThread(threadSlug);
            }

            @Override
            public int getBatchSize() {
                return posts.size();
            }
        });

        return new ResponseEntity(posts, HttpStatus.CREATED);
    }

    public ResponseEntity renameThread(String slug, int id, Thread thread) {

        String sql = "UPDATE Thread SET message = ?, title = ? WHERE slug = ? OR id = ?";

        int rowsAffected = jdbcTemplate.update(sql, thread.getMessage(), thread.getTitle(), slug, id);
        if (rowsAffected == 0) {
            return new ResponseEntity(Error.getJson("Can't find thread: " + slug), HttpStatus.NOT_FOUND);
        }

        sql = "SELECT * from Thread WHERE slug = ? or id = ?";

        thread = (Thread) jdbcTemplate.queryForObject(
                sql, new Object[] { slug, id }, new ThreadRowMapper());

        return new ResponseEntity(thread, HttpStatus.OK);

    }

    public ResponseEntity voteThread(String slug, int id, Vote vote) {

        String threadSlug;
        Thread thread;

        try {

            final String sql = "SELECT * from Thread WHERE slug = ? OR id = ?";
            thread = (Thread) jdbcTemplate.queryForObject(
                    sql, new Object[] { slug, id }, new ThreadRowMapper());

            threadSlug = thread.getSlug();

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find thread: " + slug),
                    HttpStatus.NOT_FOUND);
        }

        int voiceForUpdate = vote.getVoice();

        try {   // user has voted

            final String sqlFindVote = "SELECT voice from Vote WHERE nickname = ? AND thread = ?";
            final int voice = (int) jdbcTemplate.queryForObject(
                    sqlFindVote, new Object[]{vote.getNickname(), threadSlug}, Integer.class);

            if (vote.getVoice() == voice) { // his voice doesn't change
                return new ResponseEntity(thread, HttpStatus.OK);

            } else {    // voice changed.

                final String sqlUpdateVote = "UPDATE Vote SET voice = ? WHERE nickname = ? AND thread = ?";
                jdbcTemplate.update(sqlUpdateVote, vote.getVoice(), vote.getNickname(), threadSlug);

                voiceForUpdate = vote.getVoice() * 2;  // for example: was -1 become 1. that means we must plus 2 or -1 * (-2)

            }

        } catch (EmptyResultDataAccessException e) {    // user hasn't voted

            final String sqlInsertVote = "INSERT INTO Vote(nickname, voice, thread) VALUES(?,?,?)";

            jdbcTemplate.update(sqlInsertVote, new Object[]{vote.getNickname(), vote.getVoice(), threadSlug});
        }

        final String sql = "UPDATE Thread SET votes = votes + ? WHERE slug = ?";

        jdbcTemplate.update(sql, voiceForUpdate, threadSlug); // update threads

        thread.setVotes(thread.getVotes() + voiceForUpdate);

        return new ResponseEntity(thread, HttpStatus.OK);
    }

    public ResponseEntity getThread(String slug, int id) {

        ResponseEntity responseEntity = findThread(slug, id, jdbcTemplate);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }

        return  responseEntity;

    }

    public ResponseEntity getPosts(String slug, int id, Integer limit, Integer since, String sort, Boolean desc) {

        String threadSlug;
        try {

            final String sqlCheckForum = "SELECT slug from Thread WHERE slug = ? OR id = ?";
            threadSlug = (String) jdbcTemplate.queryForObject(sqlCheckForum, new Object[]{ slug, id }, String.class);

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find thread: " + slug),
                    HttpStatus.NOT_FOUND);
        }

        final StringBuilder sql = new StringBuilder("SELECT * from Post WHERE thread = ?");
        final List<Object> args = new ArrayList<>();
        args.add(threadSlug);

        if (since != null) {
            sql.append(" AND id");
            if (desc == Boolean.TRUE) {
                sql.append(" <");
            } else {
                sql.append(" >");
            }

            sql.append(" ?");

            args.add(since);
        }
        if (sort != null) {
            if (sort.equals("flat")) {
                sql.append(" ORDER BY created");
            }

            if (desc == Boolean.TRUE) {
                sql.append(" DESC");
            }
        }

        if (limit != null) {
            sql.append(" LIMIT ?");
            args.add(limit.intValue());
        }

        List<Post> posts = jdbcTemplate.query(sql.toString(), args.toArray(new Object[args.size()]), new PostRowMapper());

        return new ResponseEntity(posts, HttpStatus.OK);
    }

    public static ResponseEntity findThread(String slug, int id, JdbcTemplate jdbcTemplate) {

        try {

            final String sql = "SELECT * from Thread WHERE LOWER(slug COLLATE \"ucs_basic\") = LOWER(? COLLATE \"ucs_basic\") " +
                    "OR LOWER(id COLLATE \"ucs_basic\") = LOWER(? COLLATE \"ucs_basic\")";
            Thread thread = (Thread) jdbcTemplate.queryForObject(
                    sql, new Object[] { slug, id }, new ThreadRowMapper());

            return new ResponseEntity(thread, HttpStatus.OK);

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find thread: " + slug),
                    HttpStatus.NOT_FOUND);
        }

    }
}

