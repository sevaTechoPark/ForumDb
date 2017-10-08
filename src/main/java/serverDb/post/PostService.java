package serverDb.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import serverDb.forum.Forum;
import serverDb.forum.ForumRowMapper;
import serverDb.thread.Thread;
import serverDb.thread.ThreadRowMapper;
import serverDb.user.User;
import serverDb.user.UserRowMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class PostService {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    public ResponseEntity editMessage(int id, Post post) {

        final String sql = "UPDATE Post SET message = ?, isEdited = TRUE WHERE id = ?";

        int rowsAffected = jdbcTemplate.update(sql, post.getMessage(), id);
        if (rowsAffected == 0) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity("{}", HttpStatus.OK);
    }

    public ResponseEntity getThreadsInfo(int id, String[] related) {

        final ObjectMapper map = new ObjectMapper();
        final ObjectNode responseBody = map.createObjectNode();

        String sql;
        sql = "SELECT * from Post WHERE id = ?";
        Post post = (Post) jdbcTemplate.queryForObject(sql, new Object[] { id }, new PostRowMapper());
        responseBody.set("post", post.getJson());

        if (Arrays.asList(related).contains("thread")) {

            sql = "SELECT * from Thread WHERE slug = ?";
            Thread thread = (Thread) jdbcTemplate.queryForObject(sql, new Object[] { post.getThread() }, new ThreadRowMapper());
            responseBody.set("thread", thread.getJson());

        }

        if (Arrays.asList(related).contains("author")) {

            sql = "SELECT * from FUser WHERE nickname = ?";
            User user = (User) jdbcTemplate.queryForObject(sql, new Object[] { post.getAuthor() }, new UserRowMapper());
            responseBody.set("author", user.getJson());

        }

        if (Arrays.asList(related).contains("forum")) {

            sql = "SELECT * from Forum WHERE slug = ?";
            Forum forum = (Forum) jdbcTemplate.queryForObject(sql, new Object[] { post.getForum() }, new ForumRowMapper());
            responseBody.set("forum", forum.getJson());

        }

        return new ResponseEntity(responseBody, HttpStatus.OK);
    }

}

