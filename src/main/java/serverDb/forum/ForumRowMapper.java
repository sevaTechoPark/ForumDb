package serverDb.forum;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ForumRowMapper implements RowMapper {
    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
        Forum forum = new Forum();
        forum.setPosts(rs.getInt("posts"));
        forum.setThreads(rs.getInt("threads"));
        forum.setTitle(rs.getString("title"));
        forum.setUser(rs.getString("user"));

        return forum;
    }
}