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

        jdbcTemplate.update("TRUNCATE TABLE PostsThread, ForumUsers, Post, Vote, Thread, Forum, FUser RESTART IDENTITY");

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity getDatabaseInfo() {

        final ObjectMapper map = new ObjectMapper();
        final ObjectNode responseBody = map.createObjectNode();

        responseBody.put("post", jdbcTemplate.queryForObject("SELECT count(id) from Post",
                Integer.class));

        responseBody.put("thread", jdbcTemplate.queryForObject("SELECT count(id) from Thread",
                Integer.class));

        responseBody.put("forum", jdbcTemplate.queryForObject("SELECT count(id) from Forum",
                Integer.class));

        responseBody.put("user", jdbcTemplate.queryForObject("SELECT count(id) from FUser",
                Integer.class));

        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }

}

