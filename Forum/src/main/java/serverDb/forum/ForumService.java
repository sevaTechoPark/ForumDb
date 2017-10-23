package serverDb.forum;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;

import serverDb.error.Error;
import serverDb.thread.Thread;
import serverDb.thread.ThreadRowMapper;
import serverDb.user.User;
import serverDb.user.UserRowMapper;

import static serverDb.thread.ThreadService.findThread;
import static serverDb.user.UserService.findUser;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.jdbc.core.JdbcTemplate;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.List;


@Service
public class ForumService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ResponseEntity createForum(Forum forum) {

        try {

//          **************************************find user**************************************
            User user = findUser(forum.getUser(), jdbcTemplate);
            if (user == null) {
                return new ResponseEntity(Error.getJson("Can't find user with nickname: " + forum.getUser()),
                        HttpStatus.NOT_FOUND);
            }
//          **************************************find user**************************************

            forum.setUser(user.getNickname());

            final String sql = "INSERT INTO Forum(slug, title, \"user\", userId) VALUES(?,?,?,?)";
            jdbcTemplate.update(sql, new Object[] { forum.getSlug(), forum.getTitle(), forum.getUser(), user.getId() });

            return new ResponseEntity(forum, HttpStatus.CREATED);

        } catch (DuplicateKeyException e) {

            Forum duplicateForum = findForum(forum.getSlug(), jdbcTemplate);

            return new ResponseEntity(duplicateForum, HttpStatus.CONFLICT);

        } catch (DataIntegrityViolationException e) {

            return new ResponseEntity(Error.getJson("Can't find user with nickname: " + forum.getUser()),
                    HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity createThread(String forum_slug, Thread thread) {

//      **************************************find user**************************************
        User user = findUser(thread.getAuthor(), jdbcTemplate);
        if (user == null) {
            return new ResponseEntity(Error.getJson("Can't find user with nickname: " + thread.getAuthor()),
                    HttpStatus.NOT_FOUND);
        }
//      **************************************find user**************************************

        thread.setAuthor(user.getNickname());

//      **************************************find forum**************************************
        Forum forum = findForum(forum_slug, jdbcTemplate);
        if (forum == null) {
            return new ResponseEntity(Error.getJson("Can't find forum: " + forum_slug),
                    HttpStatus.NOT_FOUND);
        }
//      **************************************find forum**************************************

        int id = (int) jdbcTemplate.queryForObject("SELECT nextval('thread_id_seq')", Integer.class);
        try {
            final String sql = "INSERT INTO Thread(slug, title, author, message, created, forum, id, userId, forumId) VALUES(?,?,?,?,?,?,?,?,?)";
            jdbcTemplate.update(sql, new Object[] { thread.getSlug(), thread.getTitle(), thread.getAuthor(),
                    thread.getMessage(), thread.getCreatedTimestamp(), forum.getSlug(), id, user.getId(), forum.getId()});
        } catch (DuplicateKeyException e) {

            Thread duplicateThread = findThread(thread.getSlug(), -1, jdbcTemplate);

            return new ResponseEntity(duplicateThread, HttpStatus.CONFLICT);

        }
        thread.setId(id);
        thread.setForum(forum.getSlug());
//      UPDATE COUNT OF THREADS
        String sqlUpdate = "UPDATE Forum SET threads = threads + 1 WHERE id = ?";
        jdbcTemplate.update(sqlUpdate, forum.getId());

        final String sql = "INSERT INTO ForumUsers(userId, forumId) VALUES(?,?) " +
                "ON CONFLICT DO NOTHING";
        jdbcTemplate.update(sql, new Object[] {user.getId(), forum.getId()});

        return new ResponseEntity(thread, HttpStatus.CREATED);

    }

    public ResponseEntity getForum(String slug) {

        Forum forum = findForum(slug, jdbcTemplate);
        if (forum == null) {
            return new ResponseEntity(Error.getJson("Can't find forum: " + slug),
                    HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity(forum, HttpStatus.OK);

    }

    public ResponseEntity getThreads(String slug, Integer limit, String since, Boolean desc) throws ParseException {

//      **************************************find forum**************************************
        Forum forum = findForum(slug, jdbcTemplate);
        if (forum == null) {
            return new ResponseEntity(Error.getJson("Can't find forum: " + slug),
                    HttpStatus.NOT_FOUND);
        }
//      **************************************find forum**************************************

        final StringBuilder sql = new StringBuilder("SELECT * from Thread WHERE forumId = ?");
        final List<Object> args = new ArrayList<>();
        args.add(forum.getId());

        if (since != null) {
            sql.append(" AND created");
            if (desc == Boolean.TRUE) {
                sql.append(" <");
            } else {
                sql.append(" >");
            }
            sql.append("= '" + since + "' ");
        }
        sql.append(" ORDER BY created");
        if (desc == Boolean.TRUE) {
            sql.append(" DESC");
        }
        if (limit != null) {
            sql.append(" LIMIT ?");
            args.add(limit.intValue());
        }

        List<Thread> threads = jdbcTemplate.query(sql.toString(), args.toArray(new Object[args.size()]), new ThreadRowMapper());

        return new ResponseEntity(threads, HttpStatus.OK);
    }

    public ResponseEntity getUsers(String slug, Integer limit, String since, Boolean desc) {

//      **************************************find forum**************************************
        Forum forum = findForum(slug, jdbcTemplate);
        if (forum == null) {
            return new ResponseEntity(Error.getJson("Can't find forum: " + slug),
                    HttpStatus.NOT_FOUND);
        }
//      **************************************find forum**************************************
        int id = forum.getId();

        final StringBuilder sql = new StringBuilder("SELECT nickname, fullname, about, FUser.id, email"
                + " FROM ForumUsers JOIN FUser on(FUser.id = ForumUsers.userId) WHERE forumId = ?");
        final List<Object> args = new ArrayList<>();
        args.add(id);


        if (since != null) {
            sql.append(" AND nickname::citext");
            if (desc == Boolean.TRUE) {
                sql.append(" <");
            } else {
                sql.append(" >");
            }

            sql.append(" ?::citext");

            args.add(since);
        }
        sql.append(" ORDER BY nickname::citext");
        if (desc == Boolean.TRUE) {
            sql.append(" DESC");
        }
        if (limit != null) {
            sql.append(" LIMIT ?");
            args.add(limit.intValue());
        }


        List<User> users = jdbcTemplate.query(sql.toString(), args.toArray(new Object[args.size()]), new UserRowMapper());

        return new ResponseEntity(users, HttpStatus.OK);
    }

    public static Forum findForum(String slug, JdbcTemplate jdbcTemplate) {

        try {

            final String sql = "SELECT * from Forum WHERE slug::citext = ?::citext";
            Forum forum = jdbcTemplate.queryForObject(
                    sql, new Object[] { slug }, new ForumRowMapper());

            return forum;

        } catch (Exception e) {

            return null;

        }

    }
}




