package serverDb.thread;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Date;

public class Thread {

    private String author;
    private String forum;
    private Timestamp created;
    private String message;
    private String slug;
    private String title;
    private int votes;
    private int id;

    private boolean isParent;

    @JsonCreator
    public Thread(@JsonProperty("slug") String slug, @JsonProperty("author") String author,
                  @JsonProperty("message") String message, @JsonProperty("title") String title,
                  @JsonProperty("created") Timestamp created) {

        this.slug = slug;
        this.author = author;
        this.message = message;
        this.title = title;

        this.id = 42;   // bug tests

        if (created == null) {
            this.created = Timestamp.valueOf(ZonedDateTime.now().toLocalDateTime());
        } else {
            this.created = created;
        }

    }

    public Thread() {

    }

    public int getId() {
        return 42;
    }   // bug tests

    public String getAuthor() {
        return author;
    }

    public String getForum() {
        return forum;
    }

    public boolean getIsParent() {
        return isParent;
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

    public void setParent(boolean parent) {
        isParent = parent;
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

    @JsonIgnore
    public ObjectNode getJson(){
        final ObjectMapper map = new ObjectMapper();
        final ObjectNode node = map.createObjectNode();

        node.put("author", this.author);
        node.put("created", this.created.toInstant().toString());
        node.put("forum", this.forum);
        node.put("id", this.id);
        node.put("message", this.message);
        node.put("title", this.title);
        node.put("slug", this.slug);
        node.put("votes", this.votes);

        return node;
    }
}
