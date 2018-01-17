package serverDb.post;

import serverDb.forum.Forum;
import serverDb.thread.Thread;
import serverDb.user.User;

import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;

@Service
@Transactional
public class PostService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ResponseEntity editMessage(int id, Post post, String message) {

        if (message != null && !message.equals(post.getMessage())) {
            final String sql = "UPDATE Post SET message = ?, isEdited = TRUE WHERE id = ?";
            jdbcTemplate.update(sql, message, id);
            post.setMessage(message);
            post.setEdited(Boolean.TRUE);
        }

        return ResponseEntity.status(HttpStatus.OK).body(post);
    }

    public ResponseEntity getPost(int id, String[] related) {

        final ObjectMapper map = new ObjectMapper();
        final ObjectNode responseBody = map.createObjectNode();

        Post post = findPost(id);

        responseBody.set("post", post.getJson());

        if (Arrays.asList(related).contains("thread")) {

            final String sqlThread = "SELECT author, forum, id, message, slug, title, votes, created from Thread WHERE id = ?";
            Thread thread = jdbcTemplate.queryForObject(sqlThread, serverDb.fasterMappers.ThreadRowMapper.INSTANCE,
                    post.getThread());

            responseBody.set("thread", thread.getJson());

        }

        if (Arrays.asList(related).contains("user")) {

            final String sqlUser = "SELECT nickname, fullname, about, email from FUser WHERE nickname = ?";
            User user = jdbcTemplate.queryForObject(sqlUser, serverDb.fasterMappers.UserRowMapper.INSTANCE,
                    post.getAuthor());

            responseBody.set("author", user.getJson());

        }

        if (Arrays.asList(related).contains("forum")) {

            final String sqlForum = "SELECT posts, slug, threads, title, \"user\" from Forum WHERE id = ?";
            Forum forum = jdbcTemplate.queryForObject(sqlForum, serverDb.fasterMappers.ForumRowMapper.INSTANCE,
                    post.getForumId());

            responseBody.set("forum", forum.getJson());

        }

        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }

    public Post findPost(int id) {

        String sql = "SELECT author, created, forum, id, isEdited, message, parent, thread, forumId from Post WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, PostRowMapper.INSTANCE, id);
    }
}


