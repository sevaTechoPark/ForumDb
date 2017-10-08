package serverDb.user;

import serverDb.error.Error;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserService{

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ResponseEntity createUser(User user) {

        try {

            final String sql = "INSERT INTO FUser(nickname, email, fullname, about) VALUES(?,?,?,?)";
            jdbcTemplate.update(sql, new Object[]{user.getNickname(), user.getEmail(), user.getFullname(), user.getAbout()});

            return new ResponseEntity(user, HttpStatus.CREATED); // 201

        } catch (DuplicateKeyException e) {

            final String sql = "SELECT * from FUser WHERE nickname = ? OR email = ?";
            User duplicateUser = (User) jdbcTemplate.queryForObject(
                    sql, new Object[] { user.getNickname(), user.getEmail() }, new UserRowMapper());

            return new ResponseEntity(duplicateUser, HttpStatus.CONFLICT); // 409
        }
    }

    public ResponseEntity renameUser(User user) {

        try {

            final String sql = "UPDATE FUser SET email = ?, fullname = ?, about = ? WHERE nickname = ?";
            int rowsAffected = jdbcTemplate.update(sql, user.getEmail(), user.getFullname(), user.getAbout(), user.getNickname());
            if (rowsAffected == 0) {
                return new ResponseEntity(Error.getJson("Can't find user with nickname: " + user.getNickname()),
                        HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity(user, HttpStatus.OK);

        } catch (DuplicateKeyException e) {

            return new ResponseEntity(Error.getJson("this email has already existed"), HttpStatus.CONFLICT); // 409
        }
    }

    public ResponseEntity getUser(String nickname) {

        try {

            final String sql = "SELECT * from FUser WHERE nickname = ?";
            User user = (User) jdbcTemplate.queryForObject(
                    sql, new Object[]{nickname}, new UserRowMapper());

            return new ResponseEntity(user, HttpStatus.OK);

        } catch (EmptyResultDataAccessException emptyResultDataAccessException) {

            return new ResponseEntity(Error.getJson("Can't find user with nickname: " + nickname),
                    HttpStatus.NOT_FOUND);

        }
    }

}

