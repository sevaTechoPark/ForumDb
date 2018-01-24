package serverDb.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class User {

    private String email;
    private String about;
    private String fullname;
    private String nickname;
    private int id;

    @JsonCreator
    public User(@JsonProperty("email") String email, @JsonProperty("about") String about,
                @JsonProperty("fullname") String fullname) {

        this.email = email;
        this.fullname = fullname;
        this.about = about;

    }

    public User() {

    }

    @JsonIgnore
    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getAbout() {
        return about;
    }

    public String getFullname() {
        return fullname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

}

