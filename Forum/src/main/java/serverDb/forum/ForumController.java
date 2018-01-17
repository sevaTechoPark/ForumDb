package serverDb.forum;

import serverDb.thread.Thread;
import serverDb.thread.ThreadService;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/api/forum")
public class ForumController {

    private ForumService forumService;

    private ThreadService threadService;

    @Autowired
    public ForumController(ForumService forumService, ThreadService threadService) {
        this.forumService = forumService;
        this.threadService = threadService;
    }

    @PostMapping(path = "/create")
    public ResponseEntity createForum(@RequestBody Forum forum) {
        try {
            return forumService.createForum(forum);

        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(forumService.getForum(forum.getSlug()));

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");

        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }
    }

    @PostMapping(path = "/{slug}/create")
    public ResponseEntity createThread(@PathVariable("slug") String forum_slug, @RequestBody Thread thread) {
        try {
            return forumService.createThread(forum_slug, thread);
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(threadService.getThread(thread.getSlug()));
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }

    }

    @GetMapping(path = "/{slug}/details")
    public ResponseEntity getForum(@PathVariable("slug") String slug) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(forumService.getForum(slug));
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }
    }

    @GetMapping(path = "/{slug}/threads")
    public ResponseEntity getThreads(@PathVariable("slug") String slug, @RequestParam(value = "limit", required = false) Integer limit,
                                     @RequestParam(value = "since", required = false) String since,
                                     @RequestParam(value = "desc", required = false) Boolean desc) throws ParseException {
        if (desc == null) {
            desc = Boolean.FALSE;
        }

        try {
            return forumService.getThreads(slug, limit, since, desc);
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }

    }

    @GetMapping(path = "/{slug}/users")
    public ResponseEntity getUsers(@PathVariable("slug") String slug, @RequestParam(value = "limit", required = false) Integer limit,
                                   @RequestParam(value = "since", required = false) String since,
                                   @RequestParam(value = "desc", required = false) Boolean desc) {
        if (desc == null) {
            desc = Boolean.FALSE;
        }

        try {
            return forumService.getUsers(slug, limit, since, desc);
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }
    }
}