package serverDb.vote;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class Vote {

    private String nickname;
    private int voice;
    private int id;

    @JsonCreator
    public Vote(@JsonProperty("nickname") String nickname, @JsonProperty("voice") int voice) {

        this.nickname = nickname;
        this.voice = voice;
    }

    public Vote() {

    }

    public int getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public int getVoice() {
        return voice;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setVoice(int voice) {
        this.voice = voice;
    }
}

