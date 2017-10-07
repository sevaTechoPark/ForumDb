package serverDb.thread;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import serverDb.Post.Post;
import serverDb.Post.PostRowMapper;


import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ThreadService {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    public ResponseEntity createPosts(String slug_or_id, List<Post> posts) {

        final String sqlFindForumSlug = "SELECT DISTINCT forum from Thread WHERE slug = ?";

        String forum_slug = jdbcTemplate.queryForObject(
                sqlFindForumSlug, new Object[] { slug_or_id }, new BeanPropertyRowMapper<Thread>(Thread.class))
                .getForum();

        final ZonedDateTime zonedDateTime = ZonedDateTime.now();
        Timestamp created = Timestamp.valueOf(zonedDateTime.toLocalDateTime());

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

            }

            @Override
            public int getBatchSize() {
                return posts.size();
            }
        });

        return new ResponseEntity("{}", HttpStatus.OK);

    }

    public ResponseEntity renameThread(String slug_or_id, Thread thread) {

        final String sql = "UPDATE thread SET message = ?, title = ? WHERE slug = ?";

        int rowsAffected = jdbcTemplate.update(sql, thread.getMessage(), thread.getTitle(), slug_or_id);
        if (rowsAffected == 0) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity("{}", HttpStatus.OK);

    }

    public ResponseEntity getThread(String slug_or_id) {

        final String sql = "SELECT * from Thread WHERE slug = ?";

        Thread thread = (Thread) jdbcTemplate.queryForObject(
                sql, new Object[] { slug_or_id }, new ThreadRowMapper());


        return new ResponseEntity(thread, HttpStatus.OK);

    }

    public ResponseEntity getPosts(String slug_or_id, String limit, String since, String sort, String desc) {

        final String sql = "SELECT * from Post WHERE thread = ?";

        List<Post> posts = jdbcTemplate.query(sql, new Object[] { slug_or_id }, new PostRowMapper());

        return new ResponseEntity(posts, HttpStatus.OK);

    }

}

