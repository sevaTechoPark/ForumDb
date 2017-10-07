package serverDb.Post;

import org.springframework.jdbc.core.RowMapper;
import serverDb.forum.Forum;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PostRowMapper implements RowMapper {
    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
        Post post = new Post();
        post.setId(rs.getInt("id"));
        post.setParent(rs.getLong("parent"));
        post.setEdited(rs.getBoolean("isEdited"));
        post.setForum(rs.getString("forum"));
        post.setAuthor(rs.getString("author"));
        post.setThread(rs.getString("thread"));
        post.setCreated(rs.getString("created"));
        post.setMessage(rs.getString("message"));

        return post;
    }
}
