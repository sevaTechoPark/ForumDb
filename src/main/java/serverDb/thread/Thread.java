package serverDb.thread;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Thread {

    private String author;
    private String forum;
    private ZonedDateTime created;
    private String createdFromDb;
    private String message;
    private String slug;
    private String title;
    private int votes;
    private int id;

    private boolean isParent;

    @JsonCreator
    public Thread(@JsonProperty("slug") String slug, @JsonProperty("author") String author,
                  @JsonProperty("message") String message, @JsonProperty("title") String title,
                  @JsonProperty("created") String created) {

        this.slug = slug;
        this.author = author;
        this.message = message;
        this.title = title;
        if(created != null) {
            this.created = ZonedDateTime.parse(created, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } else {
            this.created = null;
        }

        this.createdFromDb = null;
    }


    public Thread() {

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

    public boolean getIsParent() {
        return isParent;
    }


    @JsonIgnore
    public ZonedDateTime getCreatedZonedDateTime() {
        return created;
    }

    public String getCreated() {
        if (createdFromDb != null) {
            return createdFromDb;
        }
        return created.toString();
    }

    public String getMessage() {
        return message;
    }

    public String getSlug() {
        return slug;
    }

    public void setParent(boolean parent) {
        isParent = parent;
    }

    public String getTitle() {
        return title;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVotes() {
        return votes;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setForum(String forum) {
        this.forum = forum;
    }


//for ThreadRowMapper.
    public void setCreated(String created) {
        this.createdFromDb = created;
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

        node.put("forum", this.forum);
        node.put("author", this.author);
        node.put("slug", this.slug);
        node.put("created", this.getCreated());
        node.put("title", this.title);
        node.put("message", this.message);
        node.put("votes", this.votes);

        return node;
    }
}
