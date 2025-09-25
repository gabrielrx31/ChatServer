package Services;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import common.models.User;

import server.utils.Logger;
import server.utils.Logger.LogEvent;

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
        Logger.info(LogEvent.USER_SESSION, "LoginService initialized");
    }

    // Attempts to log in a user. If the user doesn't exist, they are created automatically.
    // Returns null if the user is already logged in elsewhere.
    public User attemptLogin(String username, String clientId) {
        User user = userHandler.getOrCreateUser(username);

        if (user == null) {
            Logger.error(LogEvent.USER_SESSION, "Login failed: Could not create or find user " + username, null);
            return null; 
        }

        if (activeUsers.containsKey(user.getId())) {
            Logger.warning(LogEvent.USER_SESSION, "Login failed. User already logged in: " + username);
            return null;
        }

        activeUsers.put(user.getId(), user);
        userSessions.put(user.getId(), clientId);
        Logger.info(LogEvent.USER_SESSION, "Login success: " + username + " with clientId: " + clientId);
        return user;
    }

    // Parses a raw login message string and calls the main login logic.
    // Expected format: "clientId|timestamp|LOGIN|username"
    public User parseAndValidateLogin(String loginMessage) {
        try {
            String[] parts = loginMessage.split("\\|");
            if (parts.length < 4 || !"LOGIN".equals(parts[2]) || !isValidTimestamp(parts[1])) {
                Logger.warning(LogEvent.USER_SESSION, "Invalid login message format: " + loginMessage);
                return null;
            }
            String clientId = parts[0];
            String username = parts[3];
            return attemptLogin(username, clientId);
        } catch (Exception e) {
            Logger.error(LogEvent.USER_SESSION, "Error parsing login message: " + loginMessage, e);
            return null;
        }
    }

    // Logs a user out based on their permanent user ID.
    public boolean logout(UUID userId) {
        User user = activeUsers.remove(userId);
        userSessions.remove(userId);
        if (user != null) {
            Logger.info(LogEvent.USER_SESSION, "Logout success: " + user.getUserName() + " is now offline");
            return true;
        } else {
            Logger.warning(LogEvent.USER_SESSION, "Logout attempted for unknown user ID: " + userId);
        }
        return false;
    }

    // A convenience method to log a user out using their temporary session ID.
    public boolean logoutByClientId(String clientId) {
        UUID userIdToLogout = null;
        for (Map.Entry<UUID, String> entry : userSessions.entrySet()) {
            if (clientId.equals(entry.getValue())) {
                userIdToLogout = entry.getKey();
                break;
            }
        }
        if (userIdToLogout != null) {
            return logout(userIdToLogout);
        }
        Logger.warning(LogEvent.USER_SESSION, "Logout attempted with unknown clientId: " + clientId);
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

    private boolean isValidTimestamp(String timestamp) {
        try {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").parse(timestamp);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
