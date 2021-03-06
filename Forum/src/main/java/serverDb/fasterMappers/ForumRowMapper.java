package serverDb.fasterMappers;

import org.springframework.jdbc.core.RowMapper;
import serverDb.forum.Forum;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ForumRowMapper implements RowMapper<Forum> {
    public static final ForumRowMapper INSTANCE = new ForumRowMapper();

    public Forum mapRow(ResultSet rs, int rowNum) throws SQLException {
        Forum forum = new Forum();

        forum.setPosts(rs.getInt("posts"));
        forum.setThreads(rs.getInt("threads"));
        forum.setTitle(rs.getString("title"));
        forum.setUser(rs.getString("user"));
        forum.setSlug(rs.getString("slug"));

        return forum;
    }
}