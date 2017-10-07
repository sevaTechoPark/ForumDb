package serverDb.forum;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class Forum {

    private String user;
    private String title;
    private String slug;
    private int threads;
    private long posts;

    @JsonCreator
    public Forum(@JsonProperty("slug") String slug, @JsonProperty("title") String title,
                 @JsonProperty("user") String user) {

        this.slug = slug;
        this.title = title;
        this.user = user;

    }

    public Forum() {

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

}
