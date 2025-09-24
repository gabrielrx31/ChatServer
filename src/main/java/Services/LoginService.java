package Services;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import common.models.User;

// This service handles user authentication, session management, and tracks active users.
public class LoginService {

    private final UserHandler userHandler;
    // Maps a user's unique ID to their User object for quick access.
    private final Map<UUID, User> activeUsers;
    // Maps a user's ID to their current client session ID.
    private final Map<UUID, String> userSessions;

    public LoginService(UserHandler userHandler) {
        this.userHandler = userHandler;
        this.activeUsers = new HashMap<>();
        this.userSessions = new HashMap<>();
        System.out.println("LoginService initialized.");
    }

    // Attempts to log in a user. If the user doesn't exist, they are created automatically.
    // Returns null if the user is already logged in elsewhere.
    public User attemptLogin(String username, String clientId) {
        User user = userHandler.getOrCreateUser(username);

        if (user == null) {
            System.out.println("Login failed: Could not create or find user " + username);
            return null; // Should not happen if getOrCreateUser is working.
        }

        // Prevents a user from having multiple simultaneous sessions.
        if (activeUsers.containsKey(user.getId())) {
            System.out.println("Login failed. User already logged in: " + username);
            return null;
        }

        // If successful, register the user as active and map their session.
        activeUsers.put(user.getId(), user);
        userSessions.put(user.getId(), clientId);
        System.out.println("Login success: " + username + " with clientId: " + clientId);
        return user;
    }

    // Parses a raw login message string and calls the main login logic.
    // Expected format: "clientId|timestamp|LOGIN|username"
    public User parseAndValidateLogin(String loginMessage) {
        try {
            String[] parts = loginMessage.split("\\|");
            if (parts.length < 4 || !"LOGIN".equals(parts[2]) || !isValidTimestamp(parts[1])) {
                System.out.println("Invalid login message format: " + loginMessage);
                return null;
            }
            String clientId = parts[0];
            String username = parts[3];
            return attemptLogin(username, clientId);
        } catch (Exception e) {
            System.err.println("Error parsing login message: " + e.getMessage());
            return null;
        }
    }

    // Logs a user out based on their permanent user ID.
    public boolean logout(UUID userId) {
        User user = activeUsers.remove(userId);
        userSessions.remove(userId);
        if (user != null) {
            System.out.println("Logout success: " + user.getUserName() + " is now offline.");
            return true;
        }
        return false;
    }

    // A convenience method to log a user out using their temporary session ID.
    public boolean logoutByClientId(String clientId) {
        UUID userIdToLogout = null;
        // Find the user ID associated with the given client ID.
        for (Map.Entry<UUID, String> entry : userSessions.entrySet()) {
            if (clientId.equals(entry.getValue())) {
                userIdToLogout = entry.getKey();
                break;
            }
        }
        if (userIdToLogout != null) {
            return logout(userIdToLogout);
        }
        return false;
    }
    
    // A passthrough method to find a user by their name via the UserHandler.
    public User getUserByUsername(String username) {
        return userHandler.getUserByUsername(username);
    }
    
    // Checks if a user is currently logged in.
    public boolean isUserActive(UUID userId) {
        return activeUsers.containsKey(userId);
    }

    // Retrieves the current session ID for a given user.
    public String getClientId(UUID userId) {
        return userSessions.get(userId);
    }

    // Simple validation to ensure the timestamp in a login message is correctly formatted.
    private boolean isValidTimestamp(String timestamp) {
        try {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").parse(timestamp);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}