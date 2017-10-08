package serverDb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import serverDb.forum.Forum;
import serverDb.forum.ForumRowMapper;
import serverDb.post.Post;
import serverDb.post.PostRowMapper;
import serverDb.thread.Thread;
import serverDb.thread.ThreadRowMapper;
import serverDb.user.User;
import serverDb.user.UserRowMapper;

import java.util.Arrays;

@Service
public class ServiceService {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    public ResponseEntity clearDatabase() {

        final String sql = "TRUNCATE TABLE Post, Vote, Thread, Forum, FUser";
        jdbcTemplate.update(sql);

        return new ResponseEntity("{}", HttpStatus.OK);
    }

    public ResponseEntity getDatabaseInfo() {

        final ObjectMapper map = new ObjectMapper();
        final ObjectNode responseBody = map.createObjectNode();

        String sql;
        sql = "SELECT count(id) from Post";
        final int post = (int) jdbcTemplate.queryForObject(sql, Integer.class);
        responseBody.put("post", post);

        sql = "SELECT count(slug) from Thread";
        final int thread = (int) jdbcTemplate.queryForObject(sql, Integer.class);
        responseBody.put("thread", thread);

        sql = "SELECT count(slug) from Forum";
        final int forum = (int) jdbcTemplate.queryForObject(sql, Integer.class);
        responseBody.put("forum", forum);

        sql = "SELECT count(nickname) from FUser";
        final int user = (int) jdbcTemplate.queryForObject(sql, Integer.class);
        responseBody.put("user", user);

        return new ResponseEntity(responseBody, HttpStatus.OK);
    }

}
