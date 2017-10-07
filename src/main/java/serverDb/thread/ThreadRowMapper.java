package serverDb.thread;

import org.springframework.jdbc.core.RowMapper;
import serverDb.forum.Forum;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ThreadRowMapper implements RowMapper {
    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
        Thread thread = new Thread();
        thread.setId(rs.getInt("id"));
        thread.setVotes(rs.getInt("votes"));
        thread.setTitle(rs.getString("created"));
        thread.setMessage(rs.getString("message"));
        thread.setSlug(rs.getString("slug"));
        thread.setTitle(rs.getString("title"));
        thread.setAuthor(rs.getString("author"));

        return thread;
    }
}