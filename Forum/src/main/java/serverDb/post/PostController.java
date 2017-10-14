package serverDb.post;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/post")
public class PostController {

    private PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }


    @PostMapping(path = "/{id}/details")
    public ResponseEntity editMessage(@PathVariable("id") long id, @RequestBody Post post) {

        return postService.editMessage(id, post);
    }


    @GetMapping(path = "/{id}/details")
    public ResponseEntity getPost(@PathVariable("id") long id, @RequestParam(value = "related", required = false) String[] related) {

        if (related == null) {
            related = new String[0];
        }

        return postService.getPost(id, related);
    }



}