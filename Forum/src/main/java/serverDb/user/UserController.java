package serverDb.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(path = "/{nickname}/create")
    public ResponseEntity createUser(@PathVariable("nickname") String nickname, @RequestBody User user) {

        user.setNickname(nickname);

        try {

            return userService.createUser(user);
        } catch (DuplicateKeyException e) {

            return userService.findDuplicatedUser(user);
        }
    }

    @PostMapping(path = "/{nickname}/profile")
    public ResponseEntity renameUser(@PathVariable("nickname") String nickname, @RequestBody User user) {

        user.setNickname(nickname);

        try {
            return userService.renameUser(user);
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"message\": \"\"}");
        }
    }

    @GetMapping(path = "/{nickname}/profile")
    public ResponseEntity getUser(@PathVariable("nickname") String nickname) {

        return userService.getUser(nickname);
    }

}