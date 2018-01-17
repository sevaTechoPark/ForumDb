package serverDb.post;

import org.springframework.transaction.annotation.Transactional;
import serverDb.error.Error;
import serverDb.forum.Forum;
import serverDb.thread.Thread;
import serverDb.user.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;

@Service
public class PostService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ResponseEntity editMessage(int id, Post post) {

        String message = post.getMessage();
        ResponseEntity responseEntity = getPost(id, new String[]{"only post"});
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }
        post = (Post) responseEntity.getBody();

        try {

            if (message != null && !message.equals(post.getMessage())) {
                final String sql = "UPDATE Post SET message = ?, isEdited = TRUE WHERE id = ?";
                jdbcTemplate.update(sql, message, id);
                post.setMessage(message);
                post.setEdited(Boolean.TRUE);
            }

            return ResponseEntity.status(HttpStatus.OK).body(post);


        } catch (DataIntegrityViolationException e) {

            return ResponseEntity.status(HttpStatus.OK).body(post);

        }

    }
    
    public ResponseEntity getPost(int id, String[] related) {

        try {

            final ObjectMapper map = new ObjectMapper();
            final ObjectNode responseBody = map.createObjectNode();

            String sql = "SELECT author, created, forum, id, isEdited, message, parent, thread, forumId from Post WHERE id = ?";
            Post post = jdbcTemplate.queryForObject(sql, PostRowMapper.INSTANCE, id);

            if (Arrays.asList(related).contains("only post")) {
                return ResponseEntity.status(HttpStatus.OK).body(post);
            }

            responseBody.set("post", post.getJson());

            if (Arrays.asList(related).contains("thread")) {

                sql = "SELECT author, forum, id, message, slug, title, votes, created from Thread WHERE id = ?";
                Thread thread = jdbcTemplate.queryForObject(sql, serverDb.fasterMappers.ThreadRowMapper.INSTANCE,
                        post.getThread());

                responseBody.set("thread", thread.getJson());

            }

            if (Arrays.asList(related).contains("user")) {

                sql = "SELECT nickname, fullname, about, email from FUser WHERE nickname = ?";
                User user = jdbcTemplate.queryForObject(sql, serverDb.fasterMappers.UserRowMapper.INSTANCE,
                        post.getAuthor());

                responseBody.set("author", user.getJson());

            }

            if (Arrays.asList(related).contains("forum")) {

                sql = "SELECT posts, slug, threads, title, \"user\" from Forum WHERE id = ?";
                Forum forum = jdbcTemplate.queryForObject(sql, serverDb.fasterMappers.ForumRowMapper.INSTANCE,
                        post.getForumId());

                responseBody.set("forum", forum.getJson());

            }

            return ResponseEntity.status(HttpStatus.OK).body(responseBody);

        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
        }
    }

}


