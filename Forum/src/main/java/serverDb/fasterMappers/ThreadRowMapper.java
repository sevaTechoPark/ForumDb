package serverDb.fasterMappers;

import serverDb.thread.Thread;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ThreadRowMapper implements RowMapper<Thread> {
    public static final ThreadRowMapper INSTANCE = new ThreadRowMapper();

    public Thread mapRow(ResultSet rs, int rowNum) throws SQLException {
        Thread thread = new Thread();

        thread.setVotes(rs.getInt("votes"));
        thread.setId(rs.getInt("id"));
        thread.setCreated(rs.getTimestamp("created"));
        thread.setMessage(rs.getString("message"));
        thread.setForum(rs.getString("forum"));
        thread.setTitle(rs.getString("title"));
        thread.setAuthor(rs.getString("author"));
        thread.setSlug(rs.getString("slug"));

        return thread;
    }
}