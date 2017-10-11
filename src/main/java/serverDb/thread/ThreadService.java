package serverDb.thread;


import org.springframework.dao.DataIntegrityViolationException;
import serverDb.error.Error;
import serverDb.forum.Forum;
import serverDb.post.Post;
import serverDb.post.PostRowMapper;
import serverDb.user.User;
import serverDb.vote.Vote;

import static serverDb.forum.ForumService.findForum;
import static serverDb.user.UserService.findUser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.dao.EmptyResultDataAccessException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.*;

import java.time.ZonedDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;


@Service
public class ThreadService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ResponseEntity createPosts(String slug, int id, List<Post> posts) {

//      **************************************find thread**************************************
        ResponseEntity responseEntity = findThread(slug, id, jdbcTemplate);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }
        Thread thread = (Thread) responseEntity.getBody();
//      **************************************find thread**************************************

        int threadId = thread.getId();
        String forumSlug = thread.getForum();

        String sql;
        sql = "SELECT count(id) from Post WHERE thread = ? AND parent = 0";

        boolean flag = (0 == (int) jdbcTemplate.queryForObject(sql,new Object[] { threadId }, Integer.class))
                ? Boolean.FALSE : Boolean.TRUE;

        if(flag == Boolean.FALSE) { // may be in current posts will be parent post
            ListIterator<Post> listIter = posts.listIterator();

            while(listIter.hasNext()){

                if (listIter.next().getParent() == 0) {
                    flag = true;
                    break;
                }
            }
        }

        if (flag == Boolean.FALSE) {    // no parent message
            return new ResponseEntity(Error.getJson("Missed parent post!"),
                    HttpStatus.CONFLICT);
        }

        Timestamp created = Timestamp.valueOf(ZonedDateTime.now().toLocalDateTime());
        try {
        final List<Long> ids = jdbcTemplate.query("SELECT nextval('post_id_seq') FROM generate_series(1, ?)", new Object[]{posts.size()}, (rs, rowNum) -> rs.getLong(1));
        sql = "INSERT INTO Post(author, message, parent, thread, forum, created, id) VALUES(?,?,?,?,?,?,?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {

                    Post post = posts.get(i);

                    ps.setString(1, post.getAuthor());
                    ps.setString(2, post.getMessage());
                    ps.setLong(3, post.getParent());
                    ps.setInt(4, threadId);
                    ps.setString(5, forumSlug);
                    ps.setTimestamp(6, created);
                    ps.setLong(7, ids.get(i));

                    post.setForum(forumSlug);
                    post.setCreated(created);
                    post.setThread(threadId);
                    post.setId(ids.get(i));

                }

                @Override
                public int getBatchSize() {
                    return posts.size();
                }
            });
        } catch (DataIntegrityViolationException e) {
            return new ResponseEntity(Error.getJson("Can't find post author"), HttpStatus.NOT_FOUND);
        }

//          UPDATE COUNT OF POST
        String sqlUpdate = "UPDATE Forum SET posts = posts + ? WHERE slug = ?";
        jdbcTemplate.update(sqlUpdate, posts.size(), thread.getForum());


        return new ResponseEntity(posts, HttpStatus.CREATED);
    }

    public ResponseEntity renameThread(String slug, int id, Thread thread) {

//      **************************************find thread**************************************
        ResponseEntity responseEntity = findThread(slug, id, jdbcTemplate);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }
        Thread threadUpdated = (Thread) responseEntity.getBody();
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
            String sql = "UPDATE Thread SET message = ?, title = ? WHERE slug = ? OR id = ?";

            int rowsAffected = jdbcTemplate.update(sql, thread.getMessage(), thread.getTitle(), threadUpdated.getSlug(), threadUpdated.getId());
            if (rowsAffected == 0) {
                return new ResponseEntity(Error.getJson("Can't find thread: " + slug), HttpStatus.NOT_FOUND);
            }
            
            return new ResponseEntity(threadUpdated, HttpStatus.OK);
        } catch (DataIntegrityViolationException e) {
            return new ResponseEntity(threadUpdated, HttpStatus.OK);
        }

    }

    public ResponseEntity voteThread(String slug, int id, Vote vote) {

        String threadSlug;
        Thread thread;

        try {
            ResponseEntity responseEntity;
//          **************************************find user**************************************
            responseEntity = findUser(vote.getNickname(), jdbcTemplate);
            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                return responseEntity;
            }
//          **************************************find user**************************************

//          **********************************find thread**************************************
            responseEntity = findThread(slug, id, jdbcTemplate);
            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                return responseEntity;
            }
            thread = (Thread) responseEntity.getBody();
//          **************************************find thread**************************************

            threadSlug = thread.getSlug();

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find thread: " + slug),
                    HttpStatus.NOT_FOUND);
        } catch (DataIntegrityViolationException e) {
            return new ResponseEntity(Error.getJson("Can't find post author"), HttpStatus.NOT_FOUND);
        }

        int voiceForUpdate = vote.getVoice();

        try {   // user has voted

            final String sqlFindVote = "SELECT voice from Vote WHERE nickname = ? AND thread = ?";
            final int voice = (int) jdbcTemplate.queryForObject(
                    sqlFindVote, new Object[]{vote.getNickname(), threadSlug}, Integer.class);

            if (vote.getVoice() == voice) { // his voice doesn't change
                return new ResponseEntity(thread, HttpStatus.OK);

            } else {    // voice changed.

                final String sqlUpdateVote = "UPDATE Vote SET voice = ? WHERE LOWER(nickname COLLATE \"ucs_basic\") = LOWER(? COLLATE \"ucs_basic\") AND LOWER(thread COLLATE \"ucs_basic\") = LOWER(? COLLATE \"ucs_basic\")";
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

        int threadId;
        try {

            final String sqlCheckForum = "SELECT id from Thread WHERE slug = ? OR id = ?";
            threadId = (int) jdbcTemplate.queryForObject(sqlCheckForum, new Object[]{ slug, id }, Integer.class);

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find thread: " + slug),
                    HttpStatus.NOT_FOUND);
        }

        final StringBuilder sql = new StringBuilder("SELECT * from Post WHERE thread = ?");
        final List<Object> args = new ArrayList<>();
        args.add(threadId);

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
                sql.append(" ORDER BY created, id");
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

            final String sql = "SELECT * from Thread WHERE LOWER(slug COLLATE \"ucs_basic\") = LOWER(? COLLATE \"ucs_basic\") OR id = ?";
            Thread thread = (Thread) jdbcTemplate.queryForObject(
                    sql, new Object[] { slug, id }, new ThreadRowMapper());

            return new ResponseEntity(thread, HttpStatus.OK);

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find thread: " + slug),
                    HttpStatus.NOT_FOUND);
        }

    }

}


