package serverDb.forum;

import serverDb.thread.Thread;
import static serverDb.thread.ThreadService.findThread;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/api/forum")
public class ForumController {

    private ForumService forumService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public ForumController(ForumService forumService) {
        this.forumService = forumService;
    }

    @PostMapping(path = "/create")
    public ResponseEntity createUser(@RequestBody Forum forum) {

       return forumService.createForum(forum);
    }

    @PostMapping(path = "/{slug}/create")
    public ResponseEntity renameUser(@PathVariable("slug") String forum_slug, @RequestBody Thread thread) {
        try {
            return forumService.createThread(forum_slug, thread);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(findThread(thread.getSlug(), jdbcTemplate));
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