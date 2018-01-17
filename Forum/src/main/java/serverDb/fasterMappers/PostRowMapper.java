package serverDb.fasterMappers;

import org.springframework.jdbc.core.RowMapper;
import serverDb.post.Post;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PostRowMapper implements RowMapper<Post> {
    public static final PostRowMapper INSTANCE = new PostRowMapper();

    public Post mapRow(ResultSet rs, int rowNum) throws SQLException {
        Post post = new Post();

        post.setId(rs.getInt("id"));
        post.setForum(rs.getString("forum"));
        post.setAuthor(rs.getString("author"));
        post.setThread(rs.getInt("thread"));
        post.setCreated(rs.getTimestamp("created"));
        post.setEdited(rs.getBoolean("isEdited"));
        post.setMessage(rs.getString("message"));
        post.setParent(rs.getInt("parent"));

        return post;
    }
}
