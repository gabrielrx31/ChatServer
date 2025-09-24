package Services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import common.models.User;

// Manages the lifecycle of user accounts, such as creation and retrieval.
// This acts as the central source of truth for all registered users, whether they are online or not.
public class UserHandler {
    private Map<String, User> registeredUsers; // Maps a username to a User object.
    
    public UserHandler() {
        this.registeredUsers = new HashMap<>();
        System.out.println("UserHandler initialized.");
    }
    
    // Creates a new user if the username is not already taken.
    public User createUser(String username) {
        if (registeredUsers.containsKey(username)) {
            System.out.println("Username already exists: " + username);
            return null;
        }
        
        UUID userId = UUID.randomUUID();
        User newUser = new User(userId, username, ""); // Password is empty for now.
        registeredUsers.put(username, newUser);
        
        System.out.println("User created: " + username + " with ID: " + userId);
        return newUser;
    }
    
    // A convenience method that retrieves a user or creates them if they don't exist.
    // This is useful for a system where users don't need to register beforehand.
    public User getOrCreateUser(String username) {
        User user = registeredUsers.get(username);
        if (user == null) {
            user = createUser(username);
        }
        return user;
    }
    
    public User getUserByUsername(String username) {
        return registeredUsers.get(username);
    }

    // Iterates through all users to find one by their unique ID.
    // This can be slow with many users; a second map (UUID -> User) would optimize this.
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
  
    // Returns a copy of the user map to prevent external modification.
    public Map<String, User> getAllUsers() {
        return new HashMap<>(registeredUsers);
    }
  
    public int getTotalUserCount() {
        return registeredUsers.size();
    }
   
    public boolean removeUser(String username) {
        User removedUser = registeredUsers.remove(username);
        return removedUser != null;
    }
  
    // Allows changing a user's display name, ensuring the new name isn't already taken.
    public boolean updateUsername(String oldUsername, String newUsername) {
        User user = registeredUsers.get(oldUsername);
        if (user == null) {
            return false; // User to update doesn't exist.
        }
        
        if (registeredUsers.containsKey(newUsername)) {
            return false; // New username is already in use.
        }
        
        user.setUserName(newUsername);
        registeredUsers.remove(oldUsername);
        registeredUsers.put(newUsername, user);
        
        System.out.println("Username updated from: " + oldUsername + " to: " + newUsername);
        return true;
    }
}