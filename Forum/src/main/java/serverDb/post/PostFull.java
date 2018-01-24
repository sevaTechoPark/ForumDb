package serverDb.post;

import serverDb.forum.Forum;
import serverDb.user.User;
import serverDb.thread.Thread;

public class PostFull {

    private User author;
    private Forum forum;
    private Post post;
    private Thread thread;

    public PostFull(Post post) {
        this.post = post;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Forum getForum() {
        return forum;
    }

    public void setForum(Forum forum) {
        this.forum = forum;
    }

    public Post getPost() {
        return post;
    }

    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }
}