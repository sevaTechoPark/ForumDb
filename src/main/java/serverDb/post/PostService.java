package serverDb.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;

import serverDb.error.Error;
import serverDb.forum.Forum;
import serverDb.forum.ForumRowMapper;
import serverDb.thread.Thread;
import serverDb.thread.ThreadRowMapper;
import serverDb.user.User;
import serverDb.user.UserRowMapper;

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


    public ResponseEntity editMessage(long id, Post post) {

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

            return new ResponseEntity(post.getJson(), HttpStatus.OK);

        } catch (DataIntegrityViolationException e) {

            return new ResponseEntity(post.getJson(), HttpStatus.OK);
        }

    }

    public ResponseEntity getPost(long id, String[] related) {

        try {

            final ObjectMapper map = new ObjectMapper();
            final ObjectNode responseBody = map.createObjectNode();

            String sql;
            sql = "SELECT * from Post WHERE id = ?";
            Post post = (Post) jdbcTemplate.queryForObject(sql, new Object[] { id }, new PostRowMapper());

            if (Arrays.asList(related).contains("only post")) {
                return new ResponseEntity(post, HttpStatus.OK);
            }

            responseBody.set("post", post.getJson());

            if (Arrays.asList(related).contains("thread")) {

                sql = "SELECT * from Thread WHERE id = ?";
                Thread thread = (Thread) jdbcTemplate.queryForObject(sql, new Object[] { post.getThread() },
                        new ThreadRowMapper());

                responseBody.set("thread", thread.getJson());

            }

            if (Arrays.asList(related).contains("user")) {

                sql = "SELECT * from FUser WHERE nickname = ?";
                User user = (User) jdbcTemplate.queryForObject(sql, new Object[] { post.getAuthor() },
                        new UserRowMapper());

                responseBody.set("author", user.getJson());

            }

            if (Arrays.asList(related).contains("forum")) {

                sql = "SELECT * from Forum WHERE slug = ?";
                Forum forum = (Forum) jdbcTemplate.queryForObject(sql, new Object[] { post.getForum() },
                        new ForumRowMapper());

                responseBody.set("forum", forum.getJson());

            }

            return new ResponseEntity(responseBody, HttpStatus.OK);

        } catch (EmptyResultDataAccessException e) {

            return new ResponseEntity(Error.getJson("Can't find post: " + id),
                    HttpStatus.NOT_FOUND);

        }
    }

}


