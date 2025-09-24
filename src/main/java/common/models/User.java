package common.models;

import java.util.UUID;

// A straightforward class to hold user information.
public class User {
    private UUID id;
    private String userName;

    public User(UUID id, String userName, String password) {
        this.id = id;
        this.userName = userName;
        // The password field is included for potential future authentication features.
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