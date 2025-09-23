package Services; // Opdateret pakkenavn

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import common.models.User;

public class LoginService {
    private Map<String, User> registeredUsers;
    private Map<UUID, User> activeUsers;
    private Map<UUID, String> userSessions;

    public LoginService() {
        this.registeredUsers = new HashMap<>();
        this.activeUsers = new HashMap<>();
        this.userSessions = new HashMap<>();
        System.out.println("LoginService initialized.");
    }

    public UUID createUser(String username) {
        if (registeredUsers.containsKey(username)) {
            return null;
        }
        UUID userId = UUID.randomUUID();
        User newUser = new User(userId, username, "");
        registeredUsers.put(username, newUser);
        return userId;
    }

    public User attemptLogin(String username, String clientId) {
        User user = registeredUsers.get(username);
        if (user == null) {
            UUID newUserId = createUser(username);
            if (newUserId == null) {
                return null;
            }
            user = registeredUsers.get(username);
        }

        if (activeUsers.containsKey(user.getId())) {
            System.out.println("Login failed. User already logged in: " + username);
            return null;
        }

        activeUsers.put(user.getId(), user);
        userSessions.put(user.getId(), clientId);
        System.out.println("Login success: " + username + " with clientId: " + clientId);
        return user;
    }

    public User parseAndValidateLogin(String loginMessage) {
        try {
            String[] parts = loginMessage.split("\\|");
            if (parts.length < 4 || !"LOGIN".equals(parts[2]) || !isValidTimestamp(parts[1])) {
                return null;
            }
            String clientId = parts[0];
            String username = parts[3];
            return attemptLogin(username, clientId);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean logout(UUID userId) {
        User user = activeUsers.remove(userId);
        userSessions.remove(userId);
        return user != null;
    }

    public boolean logoutByClientId(String clientId) {
        for (Map.Entry<UUID, String> entry : userSessions.entrySet()) {
            if (clientId.equals(entry.getValue())) {
                return logout(entry.getKey());
            }
        }
        return false;
    }

    // ###############################################################
    // ## DENNE METODE MANGLER I DIN VERSION ##
    // ###############################################################
    /**
     * Finder en registreret bruger ud fra deres brugernavn.
     * @param username Brugernavnet der skal søges efter.
     * @return User-objektet hvis det findes, ellers null.
     */
    public User getUserByUsername(String username) {
        return registeredUsers.get(username);
    }
    // ###############################################################

    public boolean isUserActive(UUID userId) {
        return activeUsers.containsKey(userId);
    }

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