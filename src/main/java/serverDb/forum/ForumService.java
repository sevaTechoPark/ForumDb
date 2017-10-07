package serverDb.forum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import serverDb.thread.Thread;
import serverDb.thread.ThreadRowMapper;
import serverDb.user.User;
import serverDb.user.UserRowMapper;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ForumService {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    public ResponseEntity createForum(Forum forum) {

        final String sql = "INSERT INTO Forum(slug, title, \"user\") VALUES(?,?,?)";

        jdbcTemplate.update(sql, new Object[] { forum.getSlug(), forum.getTitle(), forum.getUser()} );

        return new ResponseEntity(forum, HttpStatus.OK);

    }

    public ResponseEntity createThread(String forum_slug, Thread thread) {

        final String sql = "INSERT INTO Thread(slug, title, author, message, created, forum) VALUES(?,?,?,?,?,?)";

        Timestamp created;
        if (thread.getCreated() == null) {
            final ZonedDateTime zonedDateTime = ZonedDateTime.now();
            created = Timestamp.valueOf(zonedDateTime.toLocalDateTime());
        } else {
            final Timestamp timestamp = new Timestamp(thread.getCreatedZonedDateTime().getLong(ChronoField.INSTANT_SECONDS) * 1000 + thread.getCreatedZonedDateTime().getLong(ChronoField.MILLI_OF_SECOND));
            created = timestamp;
        }

        jdbcTemplate.update(sql, thread.getSlug(), thread.getTitle(), thread.getAuthor(), thread.getMessage(), created, forum_slug);
        thread.setForum(forum_slug);

        return new ResponseEntity(thread, HttpStatus.OK);

    }

    public ResponseEntity getForum(String slug) {

        final String sql = "SELECT posts, threads, title, \"user\" from Forum WHERE slug = ?";

        Forum forum = (Forum) jdbcTemplate.queryForObject(
                sql, new Object[] { slug }, new ForumRowMapper());

        return new ResponseEntity(forum, HttpStatus.OK);

    }

    public ResponseEntity getThreads(String slug, String limit, String since, String desc) {

        final String sql = "SELECT * from Thread WHERE forum = ?";

        List<Thread> threads = jdbcTemplate.query(sql, new Object[] { slug }, new ThreadRowMapper());

        return new ResponseEntity(threads, HttpStatus.OK);

    }

    public ResponseEntity getUsers(String slug, String limit, String since, String desc) {
        // или пост
        final String sql = "SELECT DISTINCT nickname, email, about, fullname from FUser JOIN Thread on(author = nickname) where forum = ?";

        List<User> users = jdbcTemplate.query(sql, new Object[] { slug }, new UserRowMapper());

        return new ResponseEntity(users, HttpStatus.OK);

    }

}

