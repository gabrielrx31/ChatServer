package models.protocols;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoginProtocol {
    public static final String COMMAND_LOGIN = "LOGIN";
    public static final String COMMAND_LOGOUT = "LOGOUT";
    private static final String DELIMITER = "::";

    public String buildLoginCommand(String clientId, String username) {
        String timestamp = getCurrentTimestamp();
        return COMMAND_LOGIN + DELIMITER + timestamp + DELIMITER + clientId + DELIMITER + username;
    }

    public String buildLogoutCommand(String clientId, String username) {
        String timestamp = getCurrentTimestamp();
        return COMMAND_LOGOUT + DELIMITER + timestamp + DELIMITER + clientId + DELIMITER + username;
    }

    public String parseLoginCommand(String command) {
        if (!command.startsWith(COMMAND_LOGIN + DELIMITER)) {
            return null;
        }
        
        String[] parts = command.split(DELIMITER);
        if (parts.length < 4) {
            return null;
        }
        
        try {
            String timestamp = parts[1];
            String clientId = parts[2];
            String username = parts[3];
            
            // Validate timestamp format
            if (!isValidTimestamp(timestamp)) {
                System.out.println("Invalid timestamp format: " + timestamp);
                return null;
            }
            
            return username;
        } catch (Exception e) {
            System.out.println("Error parsing login command: " + e.getMessage());
            return null;
        }
    }

    public String parseLogoutCommand(String command) {
        if (!command.startsWith(COMMAND_LOGOUT + DELIMITER)) {
            return null;
        }
        
        String[] parts = command.split(DELIMITER);
        if (parts.length < 4) {
            return null;
        }
        
        try {
            String timestamp = parts[1];
            String clientId = parts[2];
            String username = parts[3];
            
            // Validate timestamp format
            if (!isValidTimestamp(timestamp)) {
                System.out.println("Invalid timestamp format: " + timestamp);
                return null;
            }
            
            return username;
        } catch (Exception e) {
            System.out.println("Error parsing logout command: " + e.getMessage());
            return null;
        }
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
    
    private String getCurrentTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }
}
