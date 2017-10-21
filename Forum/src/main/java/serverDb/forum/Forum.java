package serverDb.forum;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class Forum {

    private String user;
    private String title;
    private String slug;
    private int threads;
    private long posts;
    private int id;

    @JsonCreator
    public Forum(@JsonProperty("slug") String slug, @JsonProperty("title") String title,
                 @JsonProperty("user") String user) {

        this.slug = slug;
        this.title = title;
        this.user = user;

    }

    public Forum() {

    }

    public int getId() {
        return id;
    }

    public String getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public int getThreads() {
        return threads;
    }

    public long getPosts() {
        return posts;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public void setPosts(long posts) {
        this.posts = posts;
    }

    @JsonIgnore
    public ObjectNode getJson(){
        final ObjectMapper map = new ObjectMapper();
        final ObjectNode node = map.createObjectNode();

        node.put("user", this.user);
        node.put("title", this.title);
        node.put("slug", this.slug);
        node.put("threads", this.threads);
        node.put("posts", this.posts);


        return node;
    }
}
