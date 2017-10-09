package serverDb.forum;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;

import serverDb.error.Error;
import serverDb.thread.Thread;
import serverDb.thread.ThreadRowMapper;
import serverDb.user.User;
import serverDb.user.UserRowMapper;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ForumService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ResponseEntity createForum(Forum forum) {

        try {

            final String sql = "INSERT INTO Forum(slug, title, \"user\") VALUES(?,?,?)";
            jdbcTemplate.update(sql, new Object[] { forum.getSlug(), forum.getTitle(), forum.getUser() });

            return new ResponseEntity(forum, HttpStatus.CREATED);

        } catch (DuplicateKeyException e) {

            final String sql = "SELECT * from Forum WHERE slug = ?";
            Forum duplicateForum = (Forum) jdbcTemplate.queryForObject(
                    sql, new Object[] { forum.getSlug() }, new ForumRowMapper());

            return new ResponseEntity(duplicateForum, HttpStatus.CONFLICT);

        } catch (DataIntegrityViolationException e) {

            return new ResponseEntity(Error.getJson("Can't find user with nickname: " + forum.getUser()),
                    HttpStatus.NOT_FOUND);
        }

    }

    public ResponseEntity createThread(String forum_slug, Thread thread) {

        Timestamp created;
        if (thread.getCreated() == null) {

            created = Timestamp.valueOf(ZonedDateTime.now().toLocalDateTime());

        } else {

            created = new Timestamp(thread.getCreatedZonedDateTime().getLong(ChronoField.INSTANT_SECONDS) * 1000
                    + thread.getCreatedZonedDateTime().getLong(ChronoField.MILLI_OF_SECOND));;
        }

        try {

            final String sql = "INSERT INTO Thread(slug, title, author, message, created, forum) VALUES(?,?,?,?,?,?)";
            jdbcTemplate.update(sql, thread.getSlug(), thread.getTitle(), thread.getAuthor(),
                    thread.getMessage(), created, forum_slug);

            thread.setForum(forum_slug);
            thread.setCreated(created.toString());

            return new ResponseEntity(thread, HttpStatus.CREATED);

        } catch (DuplicateKeyException e) {

            final String sql = "SELECT * from Thread WHERE slug = ?";
            Thread duplicateThread = (Thread) jdbcTemplate.queryForObject(
                    sql, new Object[] { thread.getSlug() }, new ThreadRowMapper());

            return new ResponseEntity(duplicateThread, HttpStatus.CONFLICT);

        } catch (DataIntegrityViolationException e) {

            return new ResponseEntity(Error.getJson("Can't find author with nickname: " + thread.getAuthor()
            + " or forum: " + thread.getForum()), HttpStatus.NOT_FOUND);
        }



    }

    public ResponseEntity getForum(String slug) {

        try {

            final String sql = "SELECT * from Forum WHERE slug = ?";
            Forum forum = (Forum) jdbcTemplate.queryForObject(
                    sql, new Object[] { slug }, new ForumRowMapper());

            return new ResponseEntity(forum, HttpStatus.OK);

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find forum: " + slug),
                    HttpStatus.NOT_FOUND);

        }

    }

    public ResponseEntity getThreads(String slug, Integer limit, String since, Boolean desc) throws ParseException {

        try {

            final String sqlCheckForum = "SELECT slug from Forum WHERE slug = ?";
            jdbcTemplate.queryForObject(sqlCheckForum, new Object[]{ slug }, String.class);

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find forum: " + slug),
                    HttpStatus.NOT_FOUND);
        }

        final StringBuilder sql = new StringBuilder("SELECT * from Thread WHERE forum = ?");
        final List<Object> args = new ArrayList<>();
        args.add(slug);

        if (since != null) {
            sql.append(" AND created");
            if (desc == Boolean.TRUE) {
                sql.append(" <");

            } else {
                sql.append(" >");
            }
            sql.append(" = '" + since + "' ");
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

        try {

            final String sqlCheckForum = "SELECT slug from Forum WHERE slug = ?";
            jdbcTemplate.queryForObject(sqlCheckForum, new Object[]{ slug }, String.class);

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find forum: " + slug),
                    HttpStatus.NOT_FOUND);
        }

//      Получение списка пользователей, у которых есть пост или ветка обсуждения в данном форуме.
//      выводит дважды пользователей которые и ветку создавали и пост(исправить)
        final StringBuilder sql = new StringBuilder("" +
                "SELECT DISTINCT u1.nickname, u1.email, u1.about, u1.fullname " +
                "FROM FUser u1 JOIN Thread on(Thread.author = u1.nickname AND Thread.forum = ?) " +
                "UNION ALL " +
                "SELECT DISTINCT u2.nickname, u2.email, u2.about, u2.fullname " +
                "FROM FUser u2 JOIN Post on(Post.author = u2.nickname AND Post.forum = ?) ");
        final List<Object> args = new ArrayList<>();
        args.add(slug);
        args.add(slug);

        if (since != null) {
            sql.append(" where LOWER(nickname COLLATE \"ucs_basic\")");
            if (desc == Boolean.TRUE) {
                sql.append(" <");
            } else {
                sql.append(" >");
            }

            sql.append(" LOWER(? COLLATE \"ucs_basic\")");

            args.add(since);
        }
        sql.append(" ORDER BY nickname"); //  ORDER BY LOWER(nickname COLLATE "ucs_basic") doesn't work!
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

}

