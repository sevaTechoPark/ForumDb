package serverDb.Post;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import serverDb.forum.Forum;
import serverDb.thread.Thread;
import serverDb.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PostService {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    public ResponseEntity editMessage(String id, Post post) {


        return new ResponseEntity(post, HttpStatus.OK);

    }

    public ResponseEntity getThreadsInfo(String id, String[] related) {


        return new ResponseEntity("{}", HttpStatus.OK);

    }


}

