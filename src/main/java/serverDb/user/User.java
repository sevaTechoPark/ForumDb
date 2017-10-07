package serverDb.user;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class User {

    private String nickname;
    private String email;
    private String fullname;
    private String about;

    @JsonCreator
    public User(@JsonProperty("email") String email, @JsonProperty("about") String about,
                @JsonProperty("fullname") String fullname) {

        this.email = email;
        this.fullname = fullname;
        this.about = about;

    }

    public User() {

    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public String getFullname() {
        return fullname;
    }

    public String getAbout() {
        return about;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public void setAbout(String about) {
        this.about = about;
    }

}

