package serverDb.thread;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    public String getAuthor() {
        return author;
    }

    public String getForum() {
        return forum;
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

    public String getTitle() {
        return title;
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
}
