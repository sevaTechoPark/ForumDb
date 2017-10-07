package serverDb.forum;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import serverDb.thread.Thread;


@RestController
@RequestMapping("/forum")
public class ForumController {

    private ForumService forumService;

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

        return forumService.createThread(forum_slug, thread);

    }

    @GetMapping(path = "/{slug}/details")
    public ResponseEntity getForum(@PathVariable("slug") String slug) {


        return forumService.getForum(slug);

    }

    @GetMapping(path = "/{slug}/threads")
    public ResponseEntity getThreads(@PathVariable("slug") String slug, @RequestParam("limit") String limit,
                                     @RequestParam("since") String since, @RequestParam("desc") String desc) {


        return forumService.getThreads(slug, limit, since, desc);

    }

    @GetMapping(path = "/{slug}/users")
    public ResponseEntity getUsers(@PathVariable("slug") String slug, @RequestParam("limit") String limit,
                                   @RequestParam("since") String since, @RequestParam("desc") String desc) {


        return forumService.getUsers(slug, limit, since, desc);

    }

}