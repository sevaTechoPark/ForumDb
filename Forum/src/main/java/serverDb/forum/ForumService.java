package serverDb.forum;

import serverDb.error.Error;
import serverDb.thread.Thread;
import serverDb.user.User;
import serverDb.user.UserService;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
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
public class ForumService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserService userService;

    public ResponseEntity createForum(Forum forum) {

        try {

            User user = userService.findUser(forum.getUser());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));

            }

            forum.setUser(user.getNickname());

            final String sql = "INSERT INTO Forum(slug, title, \"user\", userId) VALUES(?,?,?,?)";
            jdbcTemplate.update(sql, forum.getSlug(), forum.getTitle(), forum.getUser(), user.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(forum);

        } catch (DuplicateKeyException e) {

            Forum duplicateForum = findForum(forum.getSlug());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(duplicateForum);


        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
        }
    }

    @Transactional
    public ResponseEntity createThread(String forum_slug, Thread thread) {

        User user = userService.findUser(thread.getAuthor());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
        }

        thread.setAuthor(user.getNickname());

        Forum forum = findForum(forum_slug);
        if (forum == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
        }

        return ResponseEntity.status(HttpStatus.OK).body(forum);
    }

    public ResponseEntity getThreads(String slug, Integer limit, String since, Boolean desc) throws ParseException {

//      **************************************find forum**************************************
        Forum forum = findForum(slug);
        if (forum == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
        }
//      **************************************find forum**************************************

        final StringBuilder sql = new StringBuilder("SELECT author, created, forum, id, message, slug, title, votes from Thread WHERE forumId = ?");
        final List<Object> args = new ArrayList<>(3);
        args.add(forum.getId());

        if (since != null) {
            sql.append(" AND created");
            if (desc == Boolean.TRUE) {
                sql.append(" <");
            } else {
                sql.append(" >");
            }

            sql.append("= ?::timestamp with time zone");
            args.add(since);
        }
        sql.append(" ORDER BY created");
        if (desc == Boolean.TRUE) {
            sql.append(" DESC");
        }
        if (limit != null) {
            sql.append(" LIMIT ?");
            args.add(limit);
        }

        return ResponseEntity.status(HttpStatus.OK).body(
                jdbcTemplate.query(sql.toString(), args.toArray(), serverDb.fasterMappers.ThreadRowMapper.INSTANCE)
        );
    }

    public ResponseEntity getUsers(String slug, Integer limit, String since, Boolean desc) {

//      **************************************find forum**************************************
        Forum forum = findForum(slug);
        if (forum == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Error.getJson(""));
        }
//      **************************************find forum**************************************
        final int id = forum.getId();

        final StringBuilder sql = new StringBuilder("SELECT nickname, fullname, about, email"
                + " FROM ForumUsers JOIN FUser on(FUser.id = ForumUsers.userId) WHERE forumId = ?");
        final List<Object> args = new ArrayList<>();
        args.add(id);


        if (since != null) {
            sql.append(" AND nickname::citext");
            if (desc == Boolean.TRUE) {
                sql.append(" <");
            } else {
                sql.append(" >");
            }

            sql.append(" ?::citext");

            args.add(since);
        }
        sql.append(" ORDER BY nickname::citext");
        if (desc == Boolean.TRUE) {
            sql.append(" DESC");
        }
        if (limit != null) {
            sql.append(" LIMIT ?");
            args.add(limit);
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




