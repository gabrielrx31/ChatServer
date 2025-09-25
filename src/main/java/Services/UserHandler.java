package Services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import common.models.User;
import server.utils.Logger;
import server.utils.Logger.LogEvent;

// Manages the lifecycle of user accounts, such as creation and retrieval.
// This acts as the central source of truth for all registered users, whether they are online or not.
public class UserHandler {
    private Map<String, User> registeredUsers; 
    
    public UserHandler() {
        this.registeredUsers = new HashMap<>();
        Logger.info(LogEvent.DATABASE, "UserHandler initialized");
    }
    
    // Creates a new user if the username is not already taken.
    public User createUser(String username) {
        if (registeredUsers.containsKey(username)) {
            Logger.warning(LogEvent.USER_SESSION, "Username already exists: " + username);
            return null;
        }
        
        UUID userId = UUID.randomUUID();
        User newUser = new User(userId, username, "");
        registeredUsers.put(username, newUser);
        
        Logger.info(LogEvent.USER_SESSION, "User created: " + username + " with ID: " + userId);
        return newUser;
    }
    
    // Retrieves a user or creates them if they don't exist.
    public User getOrCreateUser(String username) {
        User user = registeredUsers.get(username);
        if (user == null) {
            Logger.info(LogEvent.USER_SESSION, "User not found, creating new user: " + username);
            user = createUser(username);
        } else {
            Logger.info(LogEvent.USER_SESSION, "User retrieved: " + username + " with ID: " + user.getId());
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
            Logger.info(LogEvent.USER_SESSION, "User removed: " + username + " (ID: " + removedUser.getId() + ")");
            return true;
        } else {
            Logger.warning(LogEvent.USER_SESSION, "Attempted to remove non-existent user: " + username);
            return false;
        }
    }
  
    // Allows changing a user's display name, ensuring the new name isn't already taken.
    public boolean updateUsername(String oldUsername, String newUsername) {
        User user = registeredUsers.get(oldUsername);
        if (user == null) {
            Logger.warning(LogEvent.USER_SESSION, "Update failed: user does not exist: " + oldUsername);
            return false;
        }
        
        if (registeredUsers.containsKey(newUsername)) {
            Logger.warning(LogEvent.USER_SESSION, "Update failed: new username already in use: " + newUsername);
            return false;
        }
        
        user.setUserName(newUsername);
        registeredUsers.remove(oldUsername);
        registeredUsers.put(newUsername, user);
        
        Logger.info(LogEvent.USER_SESSION, "Username updated from " + oldUsername + " to " + newUsername);
        return true;
    }
}
