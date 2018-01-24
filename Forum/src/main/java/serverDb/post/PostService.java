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
            jdbcTemplate.update("UPDATE Post SET message = ?, isEdited = TRUE WHERE id = ?",
                    message, id);
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

            responseBody.set("thread", jdbcTemplate.queryForObject("SELECT author, forum, id, message, slug, title, votes, created from Thread WHERE id = ?",
                    serverDb.fasterMappers.ThreadRowMapper.INSTANCE, post.getThread())
                    .getJson());
        }

        if (Arrays.asList(related).contains("user")) {

            responseBody.set("author", jdbcTemplate.queryForObject("SELECT nickname, email, fullname, about from FUser WHERE nickname = ?",
                    serverDb.fasterMappers.UserRowMapper.INSTANCE, post.getAuthor())
                    .getJson());
        }

        if (Arrays.asList(related).contains("forum")) {

            responseBody.set("forum", jdbcTemplate.queryForObject("SELECT posts, slug, threads, title, \"user\" from Forum WHERE id = ?",
                    serverDb.fasterMappers.ForumRowMapper.INSTANCE,
                    post.getForumId())
                    .getJson());

        }

        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }

    public Post findPost(int id) {

        return jdbcTemplate.queryForObject("SELECT author, created, forum, id, isEdited, message, parent, thread, forumId from Post WHERE id = ?",
                PostRowMapper.INSTANCE, id);
    }
}


