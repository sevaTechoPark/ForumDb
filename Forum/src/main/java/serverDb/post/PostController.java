package serverDb.post;

import serverDb.error.Error;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity editMessage(@PathVariable("id") int id, @RequestBody Post post) {
        String message = post.getMessage();

        try {
            post = postService.findPost(id);
            return postService.editMessage(id, post, message);

        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.OK).body(post);
        }

    }


    @GetMapping(path = "/{id}/details")
    public ResponseEntity getPost(@PathVariable("id") int id,
                                  @RequestParam(value = "related", required = false) String[] related) {

        if (related == null) {
            related = new String[0];
        }

        try {
            return postService.getPost(id, related);

        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
        }
    }
}