package serverDb.post;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/post")
public class PostController {

    private PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }


    @PostMapping(path = "/{id}/details")
    public ResponseEntity editMessage(@PathVariable("id") int id, @RequestBody Post post) {

        return postService.editMessage(id, post);
    }


    @GetMapping(path = "/{id}/details")
    public ResponseEntity getThreadsInfo(@PathVariable("id") int id, @RequestParam("related") String[] related) {

        return postService.getThreadsInfo(id, related);
    }



}