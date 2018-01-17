package serverDb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class ServiceApp {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ResponseEntity clearDatabase() {

        final String sql = "TRUNCATE TABLE PathPosts, PostsThread, ForumUsers, Post, Vote, Thread, Forum, FUser RESTART IDENTITY";
        jdbcTemplate.update(sql);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity getDatabaseInfo() {

        final ObjectMapper map = new ObjectMapper();
        final ObjectNode responseBody = map.createObjectNode();

        String sql;
        sql = "SELECT count(id) from Post";
        responseBody.put("post", jdbcTemplate.queryForObject(sql, Integer.class));

        sql = "SELECT count(slug) from Thread";
        responseBody.put("thread", jdbcTemplate.queryForObject(sql, Integer.class));

        sql = "SELECT count(slug) from Forum";
        responseBody.put("forum", jdbcTemplate.queryForObject(sql, Integer.class));

        sql = "SELECT count(id) from FUser";
        responseBody.put("user", jdbcTemplate.queryForObject(sql, Integer.class));

        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }

}

