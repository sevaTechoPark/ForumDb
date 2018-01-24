package serverDb.thread;

import serverDb.post.Post;
import serverDb.user.User;
import serverDb.user.UserService;
import serverDb.vote.Vote;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/thread")
public class ThreadController {

    private ThreadService threadService;
    private UserService userService;

    @Autowired
    public ThreadController(ThreadService threadService, UserService userService) {
        this.threadService = threadService;
        this.userService = userService;
    }

    @PostMapping(path = "/{slug_or_id}/create")
    public ResponseEntity createPost(@PathVariable("slug_or_id") String slug_or_id, @RequestBody List<Post> posts) throws SQLException {
        try {
            return threadService.createPosts(slug_or_id, posts);
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }
    }


    @PostMapping(path = "/{slug_or_id}/details")
    public ResponseEntity renameThread(@PathVariable("slug_or_id") String slug_or_id, @RequestBody Thread thread) {
        Thread threadUpdated = new Thread();
        try {
            threadUpdated = threadService.getThread(slug_or_id);
            return threadService.renameThread(thread, threadUpdated);

        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        } catch (DataIntegrityViolationException e) {

            return ResponseEntity.status(HttpStatus.OK).body(threadUpdated);
        }
    }


    @PostMapping(path = "/{slug_or_id}/vote")
    public ResponseEntity voteThread(@PathVariable("slug_or_id") String slug_or_id, @RequestBody Vote vote) {
        try {
            User user = userService.getUser(vote.getNickname());
            return threadService.voteThread(slug_or_id, vote.getVoice(), user.getId());

        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }
    }


    @GetMapping(path = "/{slug_or_id}/details")
    public ResponseEntity getThread(@PathVariable("slug_or_id") String slug_or_id) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(threadService.getThread(slug_or_id));
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }

    }

    @GetMapping(path = "/{slug_or_id}/posts")
    public ResponseEntity getPosts(@PathVariable("slug_or_id") String slug_or_id,@RequestParam(value = "limit", required = false) Integer limit,
                                   @RequestParam(value = "since", required = false) Integer since,
                                   @RequestParam(value = "sort", required = false) String sort,
                                   @RequestParam(value = "desc", required = false) Boolean desc) {

        if (desc == null) {
            desc = Boolean.FALSE;
        }

        if (sort == null) {
            sort = " ";
        }

        try {
            return threadService.getPosts(slug_or_id, limit, since, sort, desc);

        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }
    }


}