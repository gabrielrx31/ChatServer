package Services;

import models.User;
import models.UserHandler;

//test
public class LoginService {
    private UserHandler userHandler;
    
    public LoginService(UserHandler userHandler) {
        this.userHandler = userHandler;
        System.out.println("LoginService initialized.");
    }
    
    public User login(String username) {
        // Get existing user or create new one via UserHandler
        User user = userHandler.getOrCreateUser(username);
        
        if (user == null) {
            System.out.println("Login failed for username: " + username);
            return null;
        }
        
        System.out.println("Login success: " + username + " with ID: " + user.getId());
        return user;
    }
    
    public boolean logout(String username) {
        User user = userHandler.getUserByUsername(username);
        if (user != null) {
            System.out.println("Logout success: " + username + " er nu offline");
            return true;
        }
        return false;
    }
    
    public boolean userExists(String username) {
        return userHandler.getUserByUsername(username) != null;
    }
   
    public User getUserByUsername(String username) {
        return userHandler.getUserByUsername(username);
    }
}