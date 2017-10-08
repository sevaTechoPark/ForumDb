package serverDb.thread;

import org.springframework.jdbc.core.RowMapper;
import serverDb.forum.Forum;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ThreadRowMapper implements RowMapper {
    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
        Thread thread = new Thread();

        thread.setVotes(rs.getInt("votes"));
        thread.setCreated(rs.getString("created"));
        thread.setMessage(rs.getString("message"));
        thread.setForum(rs.getString("forum"));
        thread.setTitle(rs.getString("title"));
        thread.setAuthor(rs.getString("author"));
        thread.setSlug(rs.getString("slug"));

        return thread;
    }
}