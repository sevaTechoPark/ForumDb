package serverDb.post;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PostRowMapper implements RowMapper {
    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
        Post post = new Post();

        post.setId(rs.getInt("id"));
        post.setForum(rs.getString("forum"));
        post.setAuthor(rs.getString("author"));
        post.setThread(rs.getString("thread"));
        post.setCreated(rs.getString("created"));
        post.setEdited(rs.getBoolean("isEdited"));
        post.setMessage(rs.getString("message"));
        post.setParent(rs.getLong("parent"));

        return post;
    }
}
