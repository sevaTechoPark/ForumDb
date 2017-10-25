package serverDb.user;

import org.springframework.transaction.annotation.Transactional;
import serverDb.error.Error;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Service
public class UserService{

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ResponseEntity createUser(User user) {

        try {

            final String sql = "INSERT INTO FUser(nickname, email, fullname, about) VALUES(?,?,?,?)";
            jdbcTemplate.update(sql, new Object[]{ user.getNickname(), user.getEmail(), user.getFullname(), user.getAbout()});

            return new ResponseEntity(user, HttpStatus.CREATED); // 201

        } catch (DuplicateKeyException e) {

            final String sql = "SELECT * FROM FUser WHERE nickname::citext =  ?::citext"
                    + " OR email::citext =  ?::citext";

            List<User> users = jdbcTemplate.query(sql, new Object[] { user.getNickname(), user.getEmail() }, new UserRowMapper());

            return new ResponseEntity(users, HttpStatus.CONFLICT); // 409
        }
    }

    public ResponseEntity renameUser(User user) {

        try {

//          **************************************find user**************************************
            User oldUser = findUser(user.getNickname(), jdbcTemplate);
            if (oldUser == null) {
                return new ResponseEntity(Error.getJson("Can't find user with nickname: " + user.getNickname()),
                        HttpStatus.NOT_FOUND);
            }
//          **************************************find user**************************************

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

            return new ResponseEntity(user, HttpStatus.OK);

        } catch (DuplicateKeyException e) {

            return new ResponseEntity(Error.getJson("this email has already existed"), HttpStatus.CONFLICT); // 409

        }
    }

    public ResponseEntity getUser(String nickname) {

        User user = findUser(nickname, jdbcTemplate);
        if (user == null) {
            return new ResponseEntity(Error.getJson("Can't find user with nickname: " + nickname),
                    HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity(user, HttpStatus.OK);
    }

    public static User findUser(String nickname, JdbcTemplate jdbcTemplate) {

        try {

            final String sql = "SELECT * from FUser WHERE  nickname::citext =  ?::citext";
            User user = jdbcTemplate.queryForObject(
                    sql, new Object[]{ nickname }, new UserRowMapper());
            return user;

        } catch (Exception e) {

            return null;
        }
    }
}

