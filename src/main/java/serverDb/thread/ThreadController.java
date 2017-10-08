package serverDb.thread;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import serverDb.Post.Post;
import serverDb.forum.Forum;
import serverDb.vote.Vote;

import java.util.List;


@RestController
@RequestMapping("/thread")
public class ThreadController {

    private ThreadService threadService;

    @Autowired
    public ThreadController(ThreadService threadService) {
        this.threadService = threadService;
    }

    @PostMapping(path = "/{slug_or_id}/create")
    public ResponseEntity createPost(@PathVariable("slug_or_id") String slug_or_id, @RequestBody List<Post> posts) {

       return threadService.createPosts(slug_or_id, posts);
    }


    @PostMapping(path = "/{slug_or_id}/details")
    public ResponseEntity renameThread(@PathVariable("slug_or_id") String slug_or_id, @RequestBody Thread thread) {

        return threadService.renameThread(slug_or_id, thread);
    }


    @PostMapping(path = "/{slug_or_id}/vote")
    public ResponseEntity voteThread(@PathVariable("slug_or_id") String slug_or_id, @RequestBody Vote vote) {

        return threadService.voteThread(slug_or_id, vote);
    }


    @GetMapping(path = "/{slug_or_id}/details")
    public ResponseEntity getThread(@PathVariable("slug_or_id") String slug_or_id) {

        return threadService.getThread(slug_or_id);
    }

    @GetMapping(path = "/{slug_or_id}/posts")
    public ResponseEntity getPosts(@PathVariable("slug_or_id") String slug_or_id, @RequestParam("limit") String limit,
                                     @RequestParam("since") String since,  @RequestParam("sort") String sort,
                                     @RequestParam("desc") String desc) {

        return threadService.getPosts(slug_or_id, limit, since, sort, desc);
    }


}