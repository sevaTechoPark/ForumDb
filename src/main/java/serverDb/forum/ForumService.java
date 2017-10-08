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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
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

    public ResponseEntity getThreads(String slug, String limit, String since, String desc) {

        try {

            final String sqlCheckForum = "SELECT slug from Forum WHERE slug = ?";
            jdbcTemplate.queryForObject(sqlCheckForum, new Object[]{slug}, String.class);

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find forum: " + slug),
                    HttpStatus.NOT_FOUND);
        }

        final String sql = "SELECT * from Thread WHERE forum = ?";
        List<Thread> threads = jdbcTemplate.query(sql, new Object[] { slug }, new ThreadRowMapper());

        return new ResponseEntity(threads, HttpStatus.OK);
    }

    public ResponseEntity getUsers(String slug, String limit, String since, String desc) {

        try {

            final String sqlCheckForum = "SELECT slug from Forum WHERE slug = ?";
            jdbcTemplate.queryForObject(sqlCheckForum, new Object[]{slug}, String.class);

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find forum: " + slug),
                    HttpStatus.NOT_FOUND);
        }

//      Получение списка пользователей, у которых есть пост или ветка обсуждения в данном форуме.
//      Скорее всего будет выводить дважды пользователей которые и ветку создавали и пост
        final String sql = "SELECT DISTINCT u1.nickname, u1.email, u1.about, u1.fullname " +
                "FROM FUser u1 JOIN Thread on(Thread.author = u1.nickname AND Thread.forum = ?) " +
                "UNION ALL " +
                "SELECT DISTINCT u2.nickname, u2.email, u2.about, u2.fullname " +
                "FROM FUser u2 JOIN Post on(Post.author = u2.nickname AND Post.forum = ?) ";

        List<User> users = jdbcTemplate.query(sql, new Object[] { slug, slug }, new UserRowMapper());

        return new ResponseEntity(users, HttpStatus.OK);
    }

}

