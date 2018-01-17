package serverDb.user;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class UserService{

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ResponseEntity createUser(User user) {

        final String sql = "INSERT INTO FUser(nickname, email, fullname, about) VALUES(?,?,?,?)";
        jdbcTemplate.update(sql, user.getNickname(), user.getEmail(), user.getFullname(), user.getAbout());

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    public ResponseEntity findDuplicatedUser(User user) {
        final String sql = "SELECT nickname, fullname, about, email FROM FUser WHERE nickname::citext =  ?::citext"
                + " OR email::citext =  ?::citext";

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                jdbcTemplate.query(sql, serverDb.fasterMappers.UserRowMapper.INSTANCE, user.getNickname(), user.getEmail())
        );
    }

    public ResponseEntity renameUser(User user) {

        User oldUser = findUser(user.getNickname());
        if (oldUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
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
    }

    public ResponseEntity getUser(String nickname) {
        User user = findUser(nickname);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");

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

