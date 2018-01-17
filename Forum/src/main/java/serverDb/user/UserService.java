package serverDb.user;

import serverDb.error.Error;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService{

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ResponseEntity createUser(User user) {

        try {

            final String sql = "INSERT INTO FUser(nickname, email, fullname, about) VALUES(?,?,?,?)";
            jdbcTemplate.update(sql, user.getNickname(), user.getEmail(), user.getFullname(), user.getAbout());

            return ResponseEntity.status(HttpStatus.CREATED).body(user);

        } catch (DuplicateKeyException e) {

            final String sql = "SELECT * FROM FUser WHERE nickname::citext =  ?::citext"
                    + " OR email::citext =  ?::citext";

            List<User> users = jdbcTemplate.query(sql, UserRowMapper.INSTANCE, user.getNickname(), user.getEmail());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(users);
        }
    }

    public ResponseEntity renameUser(User user) {

        try {

            User oldUser = findUser(user.getNickname());
            if (oldUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
            }

            if (user.getEmail() == null) {
                user.setEmail(oldUser.getEmail());
            }
            if (user.getAbout() == null) {
                user.setAbout(oldUser.getAbout());
            }
            if (user.getFullname() == null) {
                user.setFullname(oldUser.getFullname());
            }

            final String sql = "UPDATE FUser SET email = ?, fullname = ?, about = ? WHERE  nickname::citext =  ?::citext";
            jdbcTemplate.update(sql, user.getEmail(), user.getFullname(), user.getAbout(), user.getNickname());

            return ResponseEntity.status(HttpStatus.OK).body(user);


        } catch (DuplicateKeyException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT).body(Error.getJson(""));

        }
    }

    public ResponseEntity getUser(String nickname) {
        User user = findUser(nickname);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));

        }

        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    public User findUser(String nickname) {

        try {

            final String sql = "SELECT * from FUser WHERE  nickname::citext =  ?::citext";

            return jdbcTemplate.queryForObject(
                    sql, UserRowMapper.INSTANCE, nickname);

        } catch (EmptyResultDataAccessException e) {

            return null;
        }
    }
}

