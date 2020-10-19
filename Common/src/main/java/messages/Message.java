package messages;

import java.io.Serializable;
import java.util.UUID;

public abstract class Message implements Serializable {
    UUID token;

    public UUID getToken() {
        return token;
    }

    public void setToken(UUID token) {
        this.token = token;
    }



}
