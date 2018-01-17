package serverDb.thread;

import serverDb.post.Post;
import serverDb.vote.Vote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/thread")
public class ThreadController {

    private ThreadService threadService;

    @Autowired
    public ThreadController(ThreadService threadService) {
        this.threadService = threadService;
    }

    @PostMapping(path = "/{slug_or_id}/create")
    public ResponseEntity createPost(@PathVariable("slug_or_id") String slug_or_id, @RequestBody List<Post> posts) throws SQLException {

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

        return threadService.getPosts(slug_or_id, limit, since, sort, desc);
    }


}