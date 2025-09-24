package models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import common.models.User;

public class UserHandler {
    private Map<String, User> registeredUsers; // username -> User
    
    // Constructor
    public UserHandler() {
        this.registeredUsers = new HashMap<>();
        System.out.println("UserHandler initialized.");
    }
    
    public User createUser(String username) {
        // Check if username is already taken
        if (registeredUsers.containsKey(username)) {
            System.out.println("Username already exists: " + username);
            return null; // Username already exists
        }
        
        // Generate a new user ID and create User object
        UUID userId = UUID.randomUUID();
        User newUser = new User(userId, username, ""); // Empty password for username-only login
        registeredUsers.put(username, newUser);
        
        System.out.println("Bruger oprettet: " + username + " med ID: " + userId);
        return newUser;
    }
    
    public User getOrCreateUser(String username) {
        User user = registeredUsers.get(username);
        if (user == null) {
            // Auto create user if not found
            user = createUser(username);
        }
        return user;
    }
    
    public User getUserByUsername(String username) {
        return registeredUsers.get(username);
    }

    public User getUserById(UUID userId) {
        for (User user : registeredUsers.values()) {
            if (user.getId().equals(userId)) {
                return user;
            }
        }
        return null;
    }
    
    public boolean userExists(String username) {
        return registeredUsers.containsKey(username);
    }
   
    public boolean userExists(UUID userId) {
        return getUserById(userId) != null;
    }
  
    public Map<String, User> getAllUsers() {
        return new HashMap<>(registeredUsers);
    }
  
    public int getTotalUserCount() {
        return registeredUsers.size();
    }
   
    public boolean removeUser(String username) {
        User removedUser = registeredUsers.remove(username);
        if (removedUser != null) {
            System.out.println("Bruger fjernet: " + username);
            return true;
        }
        return false;
    }
  
    public boolean updateUsername(String oldUsername, String newUsername) {
        // Check if old username exists
        User user = registeredUsers.get(oldUsername);
        if (user == null) {
            return false; // Old username doesn't exist
        }
        
        // Check if new username is already taken
        if (registeredUsers.containsKey(newUsername)) {
            return false; // New username already exists
        }
        
        // Update username
        user.setUserName(newUsername);
        registeredUsers.remove(oldUsername);
        registeredUsers.put(newUsername, user);
        
        System.out.println("Username opdateret fra: " + oldUsername + " til: " + newUsername);
        return true;
    }
}
