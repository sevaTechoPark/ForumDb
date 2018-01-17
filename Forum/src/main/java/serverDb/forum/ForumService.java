package serverDb.forum;

import serverDb.thread.Thread;
import serverDb.user.User;
import serverDb.user.UserService;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ForumService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserService userService;

    public ResponseEntity createForum(Forum forum) {

        User user = userService.findUser(forum.getUser());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }

        forum.setUser(user.getNickname());

        final String sql = "INSERT INTO Forum(slug, title, \"user\", userId) VALUES(?,?,?,?)";
        jdbcTemplate.update(sql, forum.getSlug(), forum.getTitle(), forum.getUser(), user.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(forum);

    }

    public ResponseEntity createThread(String forum_slug, Thread thread) {

        User user = userService.findUser(thread.getAuthor());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }

        thread.setAuthor(user.getNickname());

        Forum forum = findForum(forum_slug);
        if (forum == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }

        final String sql = "INSERT INTO Thread(slug, title, author, message, created, forum, userId, forumId) VALUES(?,?,?,?,?,?,?,?) RETURNING id";
        int id = jdbcTemplate.queryForObject(sql, Integer.class, thread.getSlug(), thread.getTitle(), thread.getAuthor(),
                thread.getMessage(), thread.getCreatedTimestamp(), forum.getSlug(), user.getId(), forum.getId());
        thread.setId(id);
        thread.setForum(forum.getSlug());

        int forumId = forum.getId();
        int userId = user.getId();

        String sqlUpdate = "UPDATE Forum SET threads = threads + 1 WHERE id = ?";
        jdbcTemplate.update(sqlUpdate, forumId);

        sqlUpdate = "INSERT INTO ForumUsers(userId, forumId) VALUES(?,?) " +
                "ON CONFLICT DO NOTHING";
        jdbcTemplate.update(sqlUpdate, userId, forumId);

        return ResponseEntity.status(HttpStatus.CREATED).body(thread);
    }

    public ResponseEntity getForum(String slug) {

        Forum forum = findForum(slug);
        if (forum == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }

        return ResponseEntity.status(HttpStatus.OK).body(forum);
    }

    public ResponseEntity getThreads(String slug, Integer limit, String since, Boolean desc) throws ParseException {

        Forum forum = findForum(slug);
        if (forum == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }

        String descOrAsc = desc ? " DESC" : " ASC";
        String moreOrLess = desc ? " <" : " >";

        final StringBuilder sql = new StringBuilder("SELECT author, created, forum, id, message, slug, title, votes from Thread WHERE forumId = ?");
        final List<Object> args = new ArrayList<>(3);

        args.add(forum.getId());

        if (since != null && limit != null) {
            sql.append(" AND created" + moreOrLess + "= ?::timestamp with time zone"
                    + " ORDER BY created" + descOrAsc + " LIMIT ?");
            args.add(since);
            args.add(limit);
        } else if (since != null) {
            sql.append(" AND created" + moreOrLess + "= ?::timestamp with time zone"
                    + " ORDER BY created" + descOrAsc);
            args.add(since);
        } else if (limit != null) {
            sql.append(" ORDER BY created" + descOrAsc + " LIMIT ?");
            args.add(limit);
        } else {
            sql.append(" ORDER BY created" + descOrAsc);
        }

        return ResponseEntity.status(HttpStatus.OK).body(
                jdbcTemplate.query(sql.toString(), args.toArray(), serverDb.fasterMappers.ThreadRowMapper.INSTANCE)
        );
    }

    public ResponseEntity getUsers(String slug, Integer limit, String since, Boolean desc) {

        Forum forum = findForum(slug);
        if (forum == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"\"}");
        }
        final int id = forum.getId();

        String descOrAsc = desc ? " DESC" : " ASC";
        String moreOrLess = desc ? " <" : " >";

        final StringBuilder sql = new StringBuilder("SELECT nickname, fullname, about, email"
                + " FROM ForumUsers JOIN FUser on(FUser.id = ForumUsers.userId) WHERE forumId = ?");
        final List<Object> args = new ArrayList<>();
        args.add(id);

        if (since != null && limit != null) {
            sql.append(" AND nickname::citext" + moreOrLess + " ?::citext ORDER BY nickname::citext" + descOrAsc
                    + " LIMIT ?");
            args.add(since);
            args.add(limit);

        } else if (since != null) {
            sql.append(" AND nickname::citext" + moreOrLess + " ?::citext ORDER BY nickname::citext" + descOrAsc);
            args.add(since);

        } else if (limit != null) {
            sql.append(" ORDER BY nickname::citext" + descOrAsc + " LIMIT ?");
            args.add(limit);

        } else {
            sql.append(" ORDER BY nickname::citext" + descOrAsc);
        }

        return ResponseEntity.status(HttpStatus.OK).body(
                jdbcTemplate.query(sql.toString(), args.toArray(), serverDb.fasterMappers.UserRowMapper.INSTANCE)
        );
    }

    public Forum findForum(String slug) {

        try {

            final String sql = "SELECT * from Forum WHERE slug::citext = ?::citext";

            return jdbcTemplate.queryForObject(
                    sql, ForumRowMapper.INSTANCE, slug);

        } catch (EmptyResultDataAccessException e) {

            return null;
        }
    }
}




