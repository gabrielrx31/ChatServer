package Services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import models.User;

public class LoginService {
    private Map<String, User> registeredUsers;
    private Map<UUID, User> activeUsers;
    private Map<UUID, String> userSessions;

    public LoginService() {
        this.registeredUsers = new HashMap<>(); //username -> User
        this.activeUsers = new HashMap<>(); //userId -> User
        this.userSessions = new HashMap<>(); //userId -> clientId

        System.out.println("LoginService initialized.");
    }

    public UUID createUser(String username){
        if (registeredUsers.containsKey(username)){
            return null; //Username already exists
        }

        UUID userId = UUID.randomUUID();
        User newUser = new User(userId, username, "");
        registeredUsers.put(username, newUser);

        System.out.println("Bruger oprettet: " + username + " med ID: " + userId);
        return userId;
    }

    public User attemptLogin(String username, String clientId){
        //Find or create user
        User user = registeredUsers.get(username);

        if (user == null){
            //Auto create user if not found
            UUID newUserId = createUser(username);
            if (newUserId == null){
                System.out.println("Login failed for username: " + username);
                return null;
            } 
            user = registeredUsers.get(username);
        }

        //Check if user is already logged in
        if (activeUsers.containsKey(user.getId())){
            System.out.println("Fail. User already logged in: " + username);
            return null;
        }

        //Login success
        activeUsers.put(user.getId(), user);
        userSessions.put(user.getId(), clientId);

        System.out.println("Login success: " + username + " with clientId: " + clientId);
        return user;
    }


    public User parseAndValidateLogin(String loginMessage){
        try{
            String[] parts = loginMessage.split("\\|"); //Split message in 4 parts (clientID|time stamp|besked type|username)
            if (parts.length < 4){
                System.out.println("Invalid login message format.");
                return null;
            }

            if (!"LOGIN".equals(parts[2])) {
                return null; //not a login message
            }

            String clientId = parts[0];
            String timestamp = parts[1];
            String username = parts[3];

            return attemptLogin(username, clientId);
        } catch (Exception e) {
            System.out.println("Error parsing login message: " + e.getMessage());
            return null;
        }
    }

     
    public boolean logout(UUID userId) {
        User user = activeUsers.remove(userId);
        userSessions.remove(userId);
        
        if (user != null) {
            System.out.println("Logout success: " + user.getUserName() + " er nu offline");
            return true;
        }
        
        return false;
    }

    
    public boolean logoutByClientId(String clientId) {
        for (Map.Entry<UUID, String> entry : userSessions.entrySet()) {
            if (clientId.equals(entry.getValue())) {
                return logout(entry.getKey());
            }
        }
        return false;
    }
    
    public boolean logoutByUsername(String username) {
        User user = registeredUsers.get(username);
        if (user != null && activeUsers.containsKey(user.getId())) {
            return logout(user.getId());
        }
        return false;
    }


    public boolean isUserActive(UUID userId) {
        return activeUsers.containsKey(userId);
    } 
    
    public boolean isUsernameActive(String username) {
        User user = registeredUsers.get(username);
        return user != null && activeUsers.containsKey(user.getId());
    }


    
    public User getActiveUser(UUID userId) {
        return activeUsers.get(userId);
    }



    public User getUserByUsername(String username) {
        return registeredUsers.get(username);
    }


    public String getClientId(UUID userId) {
        return userSessions.get(userId);
    }

    
    public User getUserByClientId(String clientId) {
        for (Map.Entry<UUID, String> entry : userSessions.entrySet()) {
            if (clientId.equals(entry.getValue())) {
                return activeUsers.get(entry.getKey());
            }
        }
        return null;
    }

     
    public Map<UUID, User> getActiveUsers() {
        return new HashMap<>(activeUsers); 
    }

    
    public int getActiveUserCount() {
        return activeUsers.size();
    }


    public int getTotalUserCount() {
        return registeredUsers.size();
    }
    
    private boolean isValidTimestamp(String timestamp) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime.parse(timestamp, formatter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
