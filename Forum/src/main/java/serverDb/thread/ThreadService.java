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
        Thread thread = findThread(slug, id, jdbcTemplate);
        if (thread == null) {
            return new ResponseEntity(Error.getJson("Can't find thread: " + slug),
                    HttpStatus.NOT_FOUND);
        }
//      **************************************find thread**************************************
        if (posts.isEmpty()) {
            return new ResponseEntity(posts, HttpStatus.CREATED);
        }

        int threadId = thread.getId();
        String forumSlug = thread.getForum();
        int forumId = thread.getForumId();

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
        sql = "INSERT INTO Post(author, message, parent, thread, forum, created, id, path, forumId, userId) VALUES(?,?,?,?,?,?,?," +
                " (SELECT path FROM Post WHERE id = ?) || ?, ?, (SELECT id FROM FUser WHERE nickname = ?))";
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
                    if (post.getParent() != 0) {
                        ps.setLong(8, post.getParent());
                    } else {
                        ps.setLong(8, ids.get(i));
                    }
                    ps.setLong(9, ids.get(i));
                    ps.setLong(10, forumId);
                    ps.setString(11, post.getAuthor());


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
        String sqlUpdate = "UPDATE Forum SET posts = posts + ? WHERE id = ?";
        jdbcTemplate.update(sqlUpdate, posts.size(), thread.getForumId());

        return new ResponseEntity(posts, HttpStatus.CREATED);
    }

    public ResponseEntity renameThread(String slug, int id, Thread thread) {

//      **************************************find thread**************************************
        Thread threadUpdated = findThread(slug, id, jdbcTemplate);
        if (threadUpdated == null) {
            return new ResponseEntity(Error.getJson("Can't find thread: " + slug),
                    HttpStatus.NOT_FOUND);
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
                return new ResponseEntity(Error.getJson("Can't find thread: " + slug), HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity(threadUpdated, HttpStatus.OK);
        } catch (DataIntegrityViolationException e) {

            return new ResponseEntity(threadUpdated, HttpStatus.OK);
        }

    }

    public ResponseEntity voteThread(String slug, int id, Vote vote) {

//      **************************************find user**************************************
        User user = findUser(vote.getNickname(), jdbcTemplate);
        if (user == null) {
            return new ResponseEntity(Error.getJson("Can't find user with nickname: " + vote.getNickname()),
                    HttpStatus.NOT_FOUND);
        }
//      **************************************find user**************************************

//      **********************************find thread**************************************
        Thread thread = findThread(slug, id, jdbcTemplate);
        if (thread == null) {
            return new ResponseEntity(Error.getJson("Can't find thread: " + slug),
                    HttpStatus.NOT_FOUND);
        }
//      **************************************find thread**************************************

        int threadId = thread.getId();
        int userId = user.getId();

        int voiceForUpdate = vote.getVoice();

        try {   // user has voted

            final String sqlFindVote = "SELECT voice from Vote WHERE userId = ? AND treadId = ?";
            final int voice = (int) jdbcTemplate.queryForObject(
                    sqlFindVote, new Object[]{userId, threadId}, Integer.class);

            if (vote.getVoice() == voice) { // his voice doesn't change
                return new ResponseEntity(thread, HttpStatus.OK);

            } else {    // voice changed.

                final String sqlUpdateVote = "UPDATE Vote SET voice = ? WHERE userId = ? AND treadId = ?";
                jdbcTemplate.update(sqlUpdateVote, vote.getVoice(), userId, threadId);

                voiceForUpdate = vote.getVoice() * 2;  // for example: was -1 become 1. that means we must plus 2 or -1 * (-2)

            }

        } catch (EmptyResultDataAccessException e) {    // user hasn't voted

            final String sqlInsertVote = "INSERT INTO Vote(userId, voice, treadId) VALUES(?,?,?)";

            jdbcTemplate.update(sqlInsertVote, new Object[]{userId, vote.getVoice(), threadId});

        }

        final String sql = "UPDATE Thread SET votes = votes + ? WHERE id = ?";

        jdbcTemplate.update(sql, voiceForUpdate, threadId); // update threads

        thread.setVotes(thread.getVotes() + voiceForUpdate);

        return new ResponseEntity(thread, HttpStatus.OK);
    }

    public ResponseEntity getThread(String slug, int id) {

        Thread thread = findThread(slug, id, jdbcTemplate);
        if (thread == null) {
            return new ResponseEntity(Error.getJson("Can't find thread: " + slug),
                    HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity(thread, HttpStatus.OK);

    }

    public ResponseEntity getPosts(String slug, int id, Integer limit, Integer since, String sort, Boolean desc) {

//      **************************************find thread**************************************
        Thread thread = findThread(slug, id, jdbcTemplate);
        if (thread == null) {
            return new ResponseEntity(Error.getJson("Can't find thread: " + slug),
                    HttpStatus.NOT_FOUND);
        }
//      **************************************find thread**************************************

        int threadId = thread.getId();
        String descOrAsc = desc ? " DESC" : " ASC";
        String moreOrLess = desc ? " <" : " >";


        final StringBuilder sql = new StringBuilder("SELECT * from Post WHERE thread = ?");
        final List<Object> args = new ArrayList<>();

        args.add(threadId);

        if (since != null) {

            if (sort != null && sort.equals("tree")) {
                sql.append(" AND path");
            } else if (sort != null && sort.equals("parent_tree")) {
                sql.append(" AND path[1]");
            } else {
                sql.append(" AND id");
            }
        }

        if (limit != null && sort != null && sort.equals("parent_tree")) {
            if (since == null) {
                sql.append(" AND path[1]");
            }
            sql.append(" IN (SELECT id FROM Post WHERE thread = ? AND parent = 0");
            args.add(threadId);
            if (since != null) {
                sql.append(" AND path[1] ");
            }
        }

        if (since != null) {

            sql.append(moreOrLess);

            if (sort != null && sort.equals("tree")) {
                sql.append(" (SELECT path FROM Post where id = ?)");
            } else if(sort != null && sort.equals("parent_tree")) {
                sql.append(" (SELECT path[1] FROM Post where id = ?)");
            } else {
                sql.append(" ?");
            }

            args.add(since);
        }


        if (sort != null) {

            if (sort.equals("flat")) {
                sql.append(" ORDER BY created " + descOrAsc + " , id");
            }

            if (sort.equals("tree")) {
                sql.append(" ORDER BY path");
            }

            if (sort.equals("parent_tree")) {

                if (limit != null) {
                    sql.append(" order by id " + descOrAsc +" LIMIT ?)");
                    args.add(limit.intValue());
                }

                sql.append(" ORDER BY path");
            }

        } else {
            sql.append(" ORDER BY id");
        }


        sql.append(descOrAsc);

        if (limit != null) {
            if (sort != null && sort.equals("parent_tree")) {
                // NO LIMIT HERE
            } else {
                sql.append(" LIMIT ?");
                args.add(limit.intValue());
            }
        }

        List<Post> posts = jdbcTemplate.query(sql.toString(), args.toArray(new Object[args.size()]), new PostRowMapper());

        return new ResponseEntity(posts, HttpStatus.OK);

    }

    public static Thread findThread(String slug, int id, JdbcTemplate jdbcTemplate) {

        try {

            final String sql = "SELECT * from Thread WHERE id = ? OR slug::citext = ?::citext";
            Thread thread = jdbcTemplate.queryForObject(
                    sql, new Object[] {id, slug}, new ThreadRowMapper());

            return thread;

        } catch (Exception e) {

            return null;
        }

    }

}

