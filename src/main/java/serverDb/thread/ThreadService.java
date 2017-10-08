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

import java.util.List;
import java.util.ListIterator;

@Service
public class ThreadService {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    public ResponseEntity createPosts(String slug_or_id, List<Post> posts) {

        boolean flag;
        String forum_slug;

        try {

            final String sqlGetThread = "SELECT * from Thread WHERE slug = ?";
            Thread thread = (Thread) jdbcTemplate.queryForObject(
                    sqlGetThread, new Object[] { slug_or_id }, new ThreadRowMapper());

            forum_slug = thread.getForum();
            flag = thread.getIsParent();

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find thread: " + slug_or_id),
                    HttpStatus.NOT_FOUND);
        }

        if(flag == false) { // may be in current posts will be parent post
            ListIterator<Post> listIter = posts.listIterator();

            while(listIter.hasNext()){

                if (listIter.next().getParent() == 0) {
                    flag = true;
                    final String sqlUpdateThread = "UPDATE Thread SET isParent = TRUE WHERE slug = ?";
                    jdbcTemplate.update(sqlUpdateThread, new Object[] { slug_or_id });
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
                ps.setString(4, slug_or_id);
                ps.setString(5, forum_slug);
                ps.setTimestamp(6, created);

                post.setForum(forum_slug);
                post.setCreated(created.toString());
                post.setThread(slug_or_id);
            }

            @Override
            public int getBatchSize() {
                return posts.size();
            }
        });

        return new ResponseEntity(posts, HttpStatus.CREATED);
    }

    public ResponseEntity renameThread(String slug_or_id, Thread thread) {

        String sql = "UPDATE Thread SET message = ?, title = ? WHERE slug = ?";

        int rowsAffected = jdbcTemplate.update(sql, thread.getMessage(), thread.getTitle(), slug_or_id);
        if (rowsAffected == 0) {
            return new ResponseEntity(Error.getJson("Can't find thread: " + slug_or_id), HttpStatus.NOT_FOUND);
        }

        sql = "SELECT * from Thread WHERE slug = ?";

        thread = (Thread) jdbcTemplate.queryForObject(
                sql, new Object[] { slug_or_id }, new ThreadRowMapper());

        return new ResponseEntity(thread, HttpStatus.OK);

    }

    public ResponseEntity voteThread(String slug_or_id, Vote vote) {

        final String sql = "UPDATE Thread SET votes = votes + ? WHERE slug = ?";

        int rowsAffected = jdbcTemplate.update(sql, 0, slug_or_id); // check exist threads
        if (rowsAffected == 0) {
            return new ResponseEntity(Error.getJson("Can't find thread: " + slug_or_id), HttpStatus.NOT_FOUND);
        }

        int voiceForUpdate = vote.getVoice();

        try {   // user has voted

            final String sqlFindVote = "SELECT voice from Vote WHERE nickname = ? AND thread = ?";
            final int voice = (int) jdbcTemplate.queryForObject(
                    sqlFindVote, new Object[]{vote.getNickname(), slug_or_id}, Integer.class);

            if (vote.getVoice() == voice) { // his voice doesn't change
                return new ResponseEntity(vote, HttpStatus.OK);

            } else {    // voice changed.

                final String sqlUpdateVote = "UPDATE Vote SET voice = ? WHERE nickname = ? AND thread = ?";
                jdbcTemplate.update(sqlUpdateVote, vote.getVoice(), vote.getNickname(), slug_or_id);

                voiceForUpdate = voice * (-2);  // for example: was -1 become 1. that means we must plus 2 or -1 * (-2)

            }

        } catch (EmptyResultDataAccessException e) {    // user hasn't voted

            final String sqlInsertVote = "INSERT INTO Vote(nickname, voice, thread) VALUES(?,?,?)";

            jdbcTemplate.update(sqlInsertVote, new Object[]{vote.getNickname(), vote.getVoice(), slug_or_id});
        }


        jdbcTemplate.update(sql, voiceForUpdate, slug_or_id); // update threads


        return new ResponseEntity(vote, HttpStatus.OK);
    }

    public ResponseEntity getThread(String slug_or_id) {

        try {

            final String sql = "SELECT * from Thread WHERE slug = ?";
            Thread thread = (Thread) jdbcTemplate.queryForObject(
                    sql, new Object[] { slug_or_id }, new ThreadRowMapper());

            return new ResponseEntity(thread, HttpStatus.OK);

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find thread: " + slug_or_id),
                    HttpStatus.NOT_FOUND);
        }

    }

    public ResponseEntity getPosts(String slug_or_id, String limit, String since, String sort, String desc) {

        try {

            final String sqlCheckForum = "SELECT slug from Thread WHERE slug = ?";
            jdbcTemplate.queryForObject(sqlCheckForum, new Object[]{ slug_or_id }, String.class);

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find thread: " + slug_or_id),
                    HttpStatus.NOT_FOUND);
        }

        final String sql = "SELECT * from Post WHERE thread = ?";
        List<Post> posts = jdbcTemplate.query(sql, new Object[] { slug_or_id }, new PostRowMapper());

        return new ResponseEntity(posts, HttpStatus.OK);
    }

}

