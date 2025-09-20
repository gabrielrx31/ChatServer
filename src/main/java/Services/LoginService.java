package Services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import models.User;

//Handles user authentication and session management for the chat server
public class LoginService {
    private Map<String, User> registeredUsers; //Map to store registered users (username -> User)
    private Map<UUID, User> activeUsers; //Map to store active logged-in users (userId -> User)
    private Map<UUID, String> userSessions; //Map to track user sessions (userId -> clientId)

    // Constructor to inilialize the LoginService
    public LoginService() {
        this.registeredUsers = new HashMap<>(); //username -> User
        this.activeUsers = new HashMap<>(); //userId -> User
        this.userSessions = new HashMap<>(); //userId -> clientId

        System.out.println("LoginService initialized.");
    }

    //Creates new user in the in the system
    public UUID createUser(String username){
        //Check if username is already taken
        if (registeredUsers.containsKey(username)){
            return null; //Username already exists
        }

        //Generate a new user ID and create User object
        UUID userId = UUID.randomUUID();
        User newUser = new User(userId, username, ""); //Empty password (Username only login)
        registeredUsers.put(username, newUser);

        System.out.println("Bruger oprettet: " + username + " med ID: " + userId);
        return userId;
    }

    //Attempts to log in a user with the given username and clientId
    public User attemptLogin(String username, String clientId){
        //try to find existing user
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

        //Login success add to active users and sessions
        activeUsers.put(user.getId(), user);
        userSessions.put(user.getId(), clientId);

        System.out.println("Login success: " + username + " with clientId: " + clientId);
        return user;
    }

    //Parses and validates a login message in the format: clientID|timestamp|LOGIN|username
    public User parseAndValidateLogin(String loginMessage){
        try{
            String[] parts = loginMessage.split("\\|"); //Split message in 4 parts (clientID|time stamp|besked type|username)
            //Check if message har enough parts
            if (parts.length < 4){
                System.out.println("Invalid login message format.");
                return null;
            }

            //Verify this is a LOGIN message
            if (!"LOGIN".equals(parts[2])) {
                System.out.println("Message is not a LOGIN type.");
                return null; //not a login message
            }

            //Extract message components
            String clientId = parts[0]; //Client identifier
            String timestamp = parts[1]; //Message timestamp
            String username = parts[3]; //Username to login

            //Validate timestamp format before attempting login
            if (!isValidTimestamp(timestamp)) {
                System.out.println("Invalid timestamp format: " + timestamp);
                return null;
            }

            //Attempt login with extracted information
            return attemptLogin(username, clientId);
        } catch (Exception e) {
            System.out.println("Error parsing login message: " + e.getMessage());
            return null;
        }
    }

     
    //Logs out a user by their UUID
    //Returns true if logout succesfull, false if user wasnt logged in
    public boolean logout(UUID userId) {
        //Remove user from active users and sessions
        User user = activeUsers.remove(userId);
        userSessions.remove(userId);
        
        if (user != null) {
            System.out.println("Logout success: " + user.getUserName() + " er nu offline");
            return true;
        }
        
        return false;
    }

    //Logs out a user by their clientId
    //Reutrns true if logout succesfull, false if client not found
    public boolean logoutByClientId(String clientId) {
        //Find userId associated with the clientId
        for (Map.Entry<UUID, String> entry : userSessions.entrySet()) {
            if (clientId.equals(entry.getValue())) {
                return logout(entry.getKey());
            }
        }
        return false;
    }
    
    //Logs out a user by their username
    //Returns true if logout succesfull, false if user not active
    public boolean logoutByUsername(String username) {
        User user = registeredUsers.get(username);
        //Check if user exists and is active
        if (user != null && activeUsers.containsKey(user.getId())) {
            return logout(user.getId());
        }
        return false;
    }


    //Checj if a user is active/logged in
    public boolean isUserActive(UUID userId) {
        return activeUsers.containsKey(userId);
    } 
    
    //Checks if a username is active/logged in
    public boolean isUsernameActive(String username) {
        User user = registeredUsers.get(username);
        return user != null && activeUsers.containsKey(user.getId());
    }

    //Gets an active user by their UUID
    public User getActiveUser(UUID userId) {
        return activeUsers.get(userId);
    }

    //Gets a registered user by their username
    public User getUserByUsername(String username) {
        return registeredUsers.get(username);
    }

    //Gets the clientId associated with a user
    public String getClientId(UUID userId) {
        return userSessions.get(userId);
    }

    //Finds a user by their clientId
    public User getUserByClientId(String clientId) {
        //Search through sessions to find matching client ID
        for (Map.Entry<UUID, String> entry : userSessions.entrySet()) {
            if (clientId.equals(entry.getValue())) {
                return activeUsers.get(entry.getKey());
            }
        }
        return null;
    }

    //Returns copy of all active users
    public Map<UUID, User> getActiveUsers() {
        return new HashMap<>(activeUsers); 
    }

    //Gets number of active logged in users
    public int getActiveUserCount() {
        return activeUsers.size();
    }

    //Gets total number of registered users
    public int getTotalUserCount() {
        return registeredUsers.size();
    }
    
    //Validates timestamp format (yyyy-MM-dd HH:mm:ss)
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
