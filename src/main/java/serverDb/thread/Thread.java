package serverDb.thread;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Thread {

    private String author;
    private String forum;
    private String created;
    private String message;
    private String slug;
    private String title;
    private int votes;
    private int id;

    @JsonCreator
    public Thread(@JsonProperty("slug") String slug, @JsonProperty("author") String author,
                  @JsonProperty("message") String message, @JsonProperty("title") String title,
                  @JsonProperty("created") String created) {

        this.slug = slug;
        this.author = author;
        this.message = message;
        this.title = title;
        this.created = created;

    }

    public Thread() {

    }

    public String getAuthor() {
        return author;
    }

    public String getForum() {
        return forum;
    }

    public int getId() {
        return id;
    }

    public String getCreated() {
        return created;
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

    public void setId(int id) {
        this.id = id;
    }

    public void setCreated(String created) {
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
