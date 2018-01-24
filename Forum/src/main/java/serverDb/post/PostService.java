package serverDb.post;

import org.springframework.transaction.annotation.Transactional;
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

        Post post = findPost(id);
        PostFull postFull = new PostFull(post);

        if (Arrays.asList(related).contains("thread")) {
            postFull.setThread(jdbcTemplate.queryForObject("SELECT author, forum, id, message, slug, title, votes, created FROM Thread WHERE id = ?",
                    serverDb.fasterMappers.ThreadRowMapper.INSTANCE, post.getThread()));
        }

        if (Arrays.asList(related).contains("user")) {
            postFull.setAuthor(jdbcTemplate.queryForObject("SELECT nickname, email, fullname, about FROM FUser WHERE nickname = ?",
                    serverDb.fasterMappers.UserRowMapper.INSTANCE, post.getAuthor()));
        }

        if (Arrays.asList(related).contains("forum")) {
            postFull.setForum(jdbcTemplate.queryForObject("SELECT posts, slug, threads, title, \"user\" FROM Forum WHERE id = ?",
                    serverDb.fasterMappers.ForumRowMapper.INSTANCE, post.getForumId()));
        }

        return ResponseEntity.status(HttpStatus.OK).body(postFull);
    }

    public Post findPost(int id) {

        return jdbcTemplate.queryForObject("SELECT author, created, forum, id, isEdited, message, parent, thread, forumId from Post WHERE id = ?",
                PostRowMapper.INSTANCE, id);
    }
}


