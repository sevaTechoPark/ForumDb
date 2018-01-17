package serverDb.forum;

import serverDb.error.Error;
import serverDb.thread.Thread;
import serverDb.thread.ThreadService;

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
    public ResponseEntity createUser(@RequestBody Forum forum) {
        try {
            return forumService.createForum(forum);

        } catch (DuplicateKeyException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT).body(forumService.findForum(forum.getSlug()));

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
        }
    }

    @PostMapping(path = "/{slug}/create")
    public ResponseEntity renameUser(@PathVariable("slug") String forum_slug, @RequestBody Thread thread) {
        try {
            return forumService.createThread(forum_slug, thread);
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(threadService.findThread(thread.getSlug()));
        }

    }

    @GetMapping(path = "/{slug}/details")
    public ResponseEntity getForum(@PathVariable("slug") String slug) {

        return forumService.getForum(slug);
    }

    @GetMapping(path = "/{slug}/threads")
    public ResponseEntity getThreads(@PathVariable("slug") String slug, @RequestParam(value = "limit", required = false) Integer limit,
                                     @RequestParam(value = "since", required = false) String since,
                                     @RequestParam(value = "desc", required = false) Boolean desc) throws ParseException {
        if (desc == null) {
            desc = Boolean.FALSE;
        }

        return forumService.getThreads(slug, limit, since, desc);
    }

    @GetMapping(path = "/{slug}/users")
    public ResponseEntity getUsers(@PathVariable("slug") String slug, @RequestParam(value = "limit", required = false) Integer limit,
                                   @RequestParam(value = "since", required = false) String since,
                                   @RequestParam(value = "desc", required = false) Boolean desc) {
        if (desc == null) {
            desc = Boolean.FALSE;
        }

        return forumService.getUsers(slug, limit, since, desc);
    }

}