package common.models;

import java.util.UUID;

public class User {
    private UUID id;
    private String userName;

    public User(UUID id, String userName, String password) {
        this.id = id;
        this.userName = userName;
        // Password parameter is kept for future use but is not currently stored.
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}