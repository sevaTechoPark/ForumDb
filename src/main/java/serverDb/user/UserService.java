package serverDb.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserService{

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ResponseEntity createUser(User user) {

        final String sql = "INSERT INTO FUser(nickname, email, fullname, about) VALUES(?,?,?,?)";
        jdbcTemplate.update(sql, new Object[] { user.getNickname(), user.getEmail(), user.getFullname(), user.getAbout()} );

        return new ResponseEntity(user, HttpStatus.OK);
    }

    public ResponseEntity renameUser(User user) {

        final String sql = "UPDATE FUser SET email = ?, fullname = ?, about = ? WHERE nickname = ?";
        int rowsAffected = jdbcTemplate.update(sql, user.getEmail(), user.getFullname(), user.getAbout(), user.getNickname());
        if (rowsAffected == 0) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity(user, HttpStatus.OK);
    }

    public ResponseEntity getUser(String nickname) {

        final String sql = "SELECT email, fullname, about from FUser WHERE nickname = ?";
        User user = (User) jdbcTemplate.queryForObject(
                sql, new Object[] { nickname }, new UserRowMapper());

        return new ResponseEntity(user, HttpStatus.OK);
    }

}

