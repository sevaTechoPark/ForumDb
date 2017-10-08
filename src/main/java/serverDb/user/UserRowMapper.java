package serverDb.user;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper {
    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();

        user.setEmail(rs.getString("email"));
        user.setAbout(rs.getString("about"));
        user.setFullname(rs.getString("fullname"));
        user.setNickname(rs.getString("nickname"));

        return user;
    }
}