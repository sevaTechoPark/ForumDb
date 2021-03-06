package serverDb.thread;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
import java.time.ZonedDateTime;

public class Thread {

    private String author;
    private String forum;
    private Timestamp created;
    private String message;
    private String slug;
    private String title;
    private int votes;
    private int id;
    private int forumId;


    @JsonCreator
    public Thread(@JsonProperty("slug") String slug, @JsonProperty("author") String author,
                  @JsonProperty("message") String message, @JsonProperty("title") String title,
                  @JsonProperty("created") Timestamp created) {

        this.slug = slug;
        this.author = author;
        this.message = message;
        this.title = title;

        if (created == null) {
            this.created = Timestamp.valueOf(ZonedDateTime.now().toLocalDateTime());
        } else {
            this.created = created;
        }

    }

    public Thread() {

    }

    @JsonIgnore
    public int getForumId() {
        return forumId;
    }

    public int getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getForum() {
        return forum;
    }

    @JsonIgnore
    public Timestamp getCreatedTimestamp() {
        return created;
    }

    public String getCreated() {
        return created.toInstant().toString();
    }

    public String getMessage() {
        return message;
    }

    public String getSlug() {
        return slug;
    }

    public int getVotes() {
        return votes;
    }

    public String getTitle() {
        return title;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setForumId(int forumId) {
        this.forumId = forumId;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setForum(String forum) {
        this.forum = forum;
    }

//for ThreadRowMapper.
    public void setCreated(Timestamp created) {
        this.created = created;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

}
