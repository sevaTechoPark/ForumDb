package serverDb.user;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper<User> {
    // public static final UserRowMapper INSTANCE = new UserRowMapper();

    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();

        user.setEmail(rs.getString("email"));
        user.setAbout(rs.getString("about"));
        user.setFullname(rs.getString("fullname"));
        user.setNickname(rs.getString("nickname"));
        user.setId(rs.getInt("id"));

        return user;
    }
}