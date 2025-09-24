package Services;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import common.models.User;
import models.UserHandler;  // Vigtigt: Sørg for at stien til User og UserHandler er korrekt

public class LoginService {

    private final UserHandler userHandler;
    private final Map<UUID, User> activeUsers;
    private final Map<UUID, String> userSessions; // Mapper userId til clientId

    public LoginService(UserHandler userHandler) {
        this.userHandler = userHandler;
        this.activeUsers = new HashMap<>();
        this.userSessions = new HashMap<>();
        System.out.println("LoginService initialized.");
    }

    /**
     * Forsøger at logge en bruger ind. Hvis brugeren ikke eksisterer, oprettes vedkommende.
     * Returnerer null hvis brugeren allerede er logget ind.
     * @param username Brugernavnet der skal logges ind.
     * @param clientId Klientens unikke ID.
     * @return User-objektet ved succes, ellers null.
     */
    public User attemptLogin(String username, String clientId) {
        // Henter eller opretter brugeren via UserHandler (logik fra 'main')
        User user = userHandler.getOrCreateUser(username);

        if (user == null) {
            System.out.println("Login failed: Could not create or find user " + username);
            return null; // Sikkerhedsforanstaltning
        }

        // Tjekker om brugeren allerede er aktiv (logik fra din branch)
        if (activeUsers.containsKey(user.getId())) {
            System.out.println("Login failed. User already logged in: " + username);
            return null;
        }

        // Registrerer brugeren som aktiv og gemmer sessionen
        activeUsers.put(user.getId(), user);
        userSessions.put(user.getId(), clientId);
        System.out.println("Login success: " + username + " with clientId: " + clientId);
        return user;
    }

    /**
     * Parser en login-besked og forsøger at logge brugeren ind.
     * Format: "clientId|timestamp|LOGIN|username"
     */
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

    /**
     * Logger en bruger ud baseret på deres UUID.
     * @param userId Brugerens ID.
     * @return true hvis brugeren blev logget ud, ellers false.
     */
    public boolean logout(UUID userId) {
        User user = activeUsers.remove(userId);
        userSessions.remove(userId);
        if (user != null) {
            System.out.println("Logout success: " + user.getUserName() + " is now offline.");
            return true;
        }
        return false;
    }

    /**
     * Logger en bruger ud baseret på deres clientId.
     * @param clientId Klientens ID.
     * @return true hvis brugeren blev logget ud, ellers false.
     */
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
        return false;
    }
    
    /**
     * Finder en bruger ud fra deres brugernavn via UserHandler.
     * @param username Brugernavnet der skal søges efter.
     * @return User-objektet hvis det findes, ellers null.
     */
    public User getUserByUsername(String username) {
        return userHandler.getUserByUsername(username);
    }
    
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