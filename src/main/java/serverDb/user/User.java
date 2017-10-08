package serverDb.user;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


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

    @JsonIgnore
    public ObjectNode getJson(){
        final ObjectMapper map = new ObjectMapper();
        final ObjectNode node = map.createObjectNode();

        node.put("nickname", this.nickname);
        node.put("email", this.email);
        node.put("fullname", this.fullname);
        node.put("about", this.about);

        return node;
    }
}

