package serverDb.thread;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.*;


@Service
public class ThreadService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
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
        sql = "SELECT postId from PostsThread WHERE threadId = ? LIMIT 1";

        List<String> withoutDublicate = new ArrayList<>();
        List<Integer> parentPostId = new ArrayList<>();

        boolean flag = Boolean.FALSE;
        try {
            int _id = (int) jdbcTemplate.queryForObject(sql,new Object[] { threadId }, Integer.class);
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
                return new ResponseEntity(Error.getJson("Missed parent post!"),
                        HttpStatus.CONFLICT);
            }

        }

        Timestamp created = Timestamp.valueOf(ZonedDateTime.now().toLocalDateTime());
        final List<Integer> ids = jdbcTemplate.query("SELECT nextval('post_id_seq') FROM generate_series(1, ?)", new Object[]{posts.size()}, (rs, rowNum) -> rs.getInt(1));
        sql = "INSERT INTO Post(author, message, parent, thread, forum, forumId, created, id, path) VALUES(?,?,?,?,?,?,?,?," +
                " (SELECT path FROM Post WHERE id = ?) || ?)";

        try(Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.NO_GENERATED_KEYS);
            for (int i = 0; i < posts.size(); i++) {
                Post post = posts.get(i);
                id = ids.get(i);

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
            return new ResponseEntity(Error.getJson("Can't find post author"), HttpStatus.NOT_FOUND);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try(Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            sql = "INSERT INTO ForumUsers(userId, forumId) VALUES( (SELECT id FROM FUser WHERE nickname = ?), ?) " +
                    "ON CONFLICT DO NOTHING";
            PreparedStatement ps = connection.prepareStatement(sql, Statement.NO_GENERATED_KEYS);

            for (int i = 0; i < withoutDublicate.size(); i++) {

                String author = withoutDublicate.get(i);

                ps.setString(1, author);
                ps.setInt(2, forumId);

                ps.addBatch();
            }
            ps.executeBatch();

        } catch (BatchUpdateException e) {
            //
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try(Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            sql = "INSERT INTO PostsThread(postId, threadId) VALUES(?,?)";
            PreparedStatement ps = connection.prepareStatement(sql, Statement.NO_GENERATED_KEYS);

            for (int i = 0; i < parentPostId.size(); i++) {

                id = ids.get(parentPostId.get(i));

                ps.setInt(1, id);
                ps.setInt(2, threadId);

                ps.addBatch();
            }
            ps.executeBatch();

        } catch (BatchUpdateException e) {
            //
        } catch (SQLException e) {
            e.printStackTrace();
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

            final String sqlFindVote = "SELECT voice from Vote WHERE userId = ? AND threadId = ?";
            final int voice = jdbcTemplate.queryForObject(
                    sqlFindVote, new Object[]{userId, threadId}, Integer.class);

            if (vote.getVoice() == voice) { // his voice doesn't change
                return new ResponseEntity(thread, HttpStatus.OK);

            } else {    // voice changed.

                final String sqlUpdateVote = "UPDATE Vote SET voice = ? WHERE userId = ? AND threadId = ?";
                jdbcTemplate.update(sqlUpdateVote, vote.getVoice(), userId, threadId);

                voiceForUpdate = vote.getVoice() * 2;  // for example: was -1 become 1. that means we must plus 2 or -1 * (-2)

            }

        } catch (EmptyResultDataAccessException e) {    // user hasn't voted

            final String sqlInsertVote = "INSERT INTO Vote(userId, voice, threadId) VALUES(?,?,?)";

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

        // START
        final StringBuilder sql = new StringBuilder();
        final List<Object> args = new ArrayList<>();

        switch (sort) {
            case "flat":
                sql.append("SELECT * from Post WHERE thread = ?");
                args.add(threadId);

                if (since != null) {
                    sql.append(" AND id");

                    sql.append(moreOrLess);

                    sql.append(" ?");

                    args.add(since);
                }

                sql.append(" ORDER BY created " + descOrAsc + " , id");

                sql.append(descOrAsc);

                if (limit != null) {
                    sql.append(" LIMIT ?");
                    args.add(limit.intValue());
                }
                break;
            case "tree":
                sql.append("SELECT * from Post WHERE thread = ?");
                args.add(threadId);

                if (since != null) {
                    sql.append(" AND path");
                    sql.append(moreOrLess);

                    sql.append(" (SELECT path FROM Post where id = ?)");
                    args.add(since);

                }

                sql.append(" ORDER BY path");

                sql.append(descOrAsc);

                if (limit != null) {
                    sql.append(" LIMIT ?");
                    args.add(limit.intValue());
                }
                break;
            case "parent_tree":
                sql.append("SELECT * from Post WHERE thread = ?");
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

                    sql.append(" (SELECT path[1] FROM Post where id = ?)");

                    args.add(since);
                }

                if (limit != null) {
                    sql.append(" order by id " + descOrAsc +" LIMIT ?)");
                    args.add(limit.intValue());
                }

                sql.append(" ORDER BY path");

                sql.append(descOrAsc);
                break;
            default:
                sql.append("SELECT * from Post WHERE thread = ?");
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
                    args.add(limit.intValue());
                }

        }
        // END

//        final StringBuilder sql = new StringBuilder("SELECT * from Post WHERE thread = ?");
//        final List<Object> args = new ArrayList<>();
//
//        args.add(threadId);
//
//        if (since != null) {
//
//            if (sort.equals("tree")) {
//                sql.append(" AND path");
//            } else if (sort.equals("parent_tree")) {
//                sql.append(" AND path[1]");
//            } else {
//                sql.append(" AND id");
//            }
//        }
//
//        if (limit != null && sort.equals("parent_tree")) {
//            if (since == null) {
//                sql.append(" AND path[1]");
//            }
//            sql.append(" IN (SELECT postId as id FROM PostsThread WHERE threadId = ? AND parent = 0");
//            args.add(threadId);
//            if (since != null) {
//                sql.append(" AND path[1]");
//            }
//        }
//
//        if (since != null) {
//
//            sql.append(moreOrLess);
//
//            if (sort.equals("tree")) {
//                sql.append(" (SELECT path FROM PostsThread where postId = ?)");
//            } else if(sort.equals("parent_tree")) {
//                sql.append(" (SELECT path[1] FROM PostsThread where postId = ?)");
//            } else {
//                sql.append(" ?");
//            }
//
//            args.add(since);
//        }
//
//
//        if (!sort.equals(" ")) {
//
//            if (sort.equals("flat")) {
//                sql.append(" ORDER BY created " + descOrAsc + " , id");
//            }
//
//            if (sort.equals("tree")) {
//                sql.append(" ORDER BY path");
//            }
//
//            if (sort.equals("parent_tree")) {
//
//                if (limit != null) {
//                    sql.append(" order by id " + descOrAsc +" LIMIT ?)");
//                    args.add(limit.intValue());
//                }
//
//                sql.append(" ORDER BY path");
//            }
//
//        } else {
//            sql.append(" ORDER BY id");
//        }
//
//
//        sql.append(descOrAsc);
//
//        if (limit != null) {
//            if (sort.equals("parent_tree")) {
//                // NO LIMIT HERE
//            } else {
//                sql.append(" LIMIT ?");
//                args.add(limit.intValue());
//            }
//        }

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

