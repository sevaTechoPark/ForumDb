package serverDb.post;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.ZonedDateTime;


public class Post {

    private int id;
    private String forum;
    private String author;
    private String thread;
    private ZonedDateTime created;
    private String createdFromDb;
    private boolean isEdited;
    private String message;
    private long parent;

    @JsonCreator
    public Post(@JsonProperty("author") String author, @JsonProperty("message") String message,
                @JsonProperty("parent") long parent) {

        this.author = author;
        this.message = message;
        this.parent = parent;

        this.createdFromDb = null;

    }

    public Post() {

    }

    public int getId() {
        return id;
    }

    public String getForum() {
        return forum;
    }

    public String getAuthor() {
        return author;
    }

    public String getCreated() {
        if (createdFromDb != null) {
            return createdFromDb;
        }
        return created.toString();
    }

    public boolean isEdited() {
        return isEdited;
    }

    public String getMessage() {
        return message;
    }

    public long getParent() {
        return parent;
    }

    public String getThread() {
        return thread;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setForum(String forum) {
        this.forum = forum;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setCreated(String created) {
        this.createdFromDb = created;
    }

    public void setEdited(boolean edited) {
        isEdited = edited;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setParent(long parent) {
        this.parent = parent;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    @JsonIgnore
    public ObjectNode getJson(){
        final ObjectMapper map = new ObjectMapper();
        final ObjectNode node = map.createObjectNode();

        node.put("id", this.id);
        node.put("forum", this.forum);
        node.put("author", this.author);
        node.put("thread", this.thread);
        node.put("created", this.getCreated());
        node.put("isEdited", this.isEdited);
        node.put("message", this.message);
        node.put("parent", this.parent);

        return node;
    }
}
