package serverDb.post;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PostRowMapper implements RowMapper<Post> {
    public Post mapRow(ResultSet rs, int rowNum) throws SQLException {
        Post post = new Post();

        post.setId(rs.getLong("id"));
        post.setUserId(rs.getLong("userId"));
        post.setForumId(rs.getLong("forumId"));
        post.setForum(rs.getString("forum"));
        post.setAuthor(rs.getString("author"));
        post.setThread(rs.getInt("thread"));
        post.setCreated(rs.getTimestamp("created"));
        post.setEdited(rs.getBoolean("isEdited"));
        post.setMessage(rs.getString("message"));
        post.setParent(rs.getLong("parent"));

        return post;
    }
}
