package serverDb.forum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import serverDb.thread.Thread;
import serverDb.user.User;

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

        List<Thread> threads = new ArrayList<Thread>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new Object[] { slug });
        for (Map row : rows) {
            Thread thread = new Thread();

            thread.setVotes((int)row.get("votes"));
            thread.setCreated((String)row.get("created"));
            thread.setMessage((String)row.get("message"));
            thread.setSlug((String)row.get("slug"));
            thread.setTitle((String)row.get("title"));
            thread.setAuthor((String)row.get("author"));
            thread.setForum(slug);

            threads.add(thread);
        }

        return new ResponseEntity(threads, HttpStatus.OK);

    }

    public ResponseEntity getUsers(String slug, String limit, String since, String desc) {
        // или пост
        final String sql = "SELECT DISTINCT nickname, email, about, fullname from FUser JOIN Thread on(author = nickname) where forum = ?";

        List<User> users = new ArrayList<User>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new Object[] { slug });
        for (Map row : rows) {
            User user = new User();
            user.setEmail((String)row.get("email"));
            user.setAbout((String)row.get("about"));
            user.setFullname((String)row.get("fullname"));
            user.setNickname((String)row.get("nickname"));
            users.add(user);
        }

        return new ResponseEntity(users, HttpStatus.OK);

    }

}

