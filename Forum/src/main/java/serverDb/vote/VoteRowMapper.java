package serverDb.vote;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class VoteRowMapper implements RowMapper<Vote> {
    public static final VoteRowMapper INSTANCE = new VoteRowMapper();

    public Vote mapRow(ResultSet rs, int rowNum) throws SQLException {
        Vote vote = new Vote();

        vote.setVoice(rs.getInt("voice"));
        vote.setId(rs.getInt("id"));

        return vote;
    }
}
