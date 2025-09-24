package common.protocols;

import java.time.LocalDateTime;
import java.util.UUID;

import common.models.MessageModel;


// Handles the creation and parsing of message-related command strings.
public class MessageProtocol {
    public static final String COMMAND_SEND_MSG = "SEND_MSG";
    public static final String MSG_TYPE_TEXT = "TEXT";
    public static final String MSG_TYPE_SYSTEM = "SYSTEM";
    private static final String DELIMITER = "::";

    // Constructs a standard text message command.
    public String buildTextMessage(String sender, UUID chatroomId, String content) {
        UUID messageId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.now();
        // The structure for a message command is defined here.
        return COMMAND_SEND_MSG + DELIMITER + messageId + DELIMITER + timestamp + DELIMITER + MSG_TYPE_TEXT + DELIMITER + sender + DELIMITER + chatroomId + DELIMITER + content;
    }

    // Constructs a system message, which always has "SYSTEM" as the sender.
    public String buildSystemMessage(String content, UUID chatroomId) {
        UUID messageId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.now();
        return COMMAND_SEND_MSG + DELIMITER + messageId + DELIMITER + timestamp + DELIMITER + MSG_TYPE_SYSTEM + DELIMITER + "SYSTEM" + DELIMITER + chatroomId + DELIMITER + content;
    }
    
    // Deconstructs a raw command string into a structured MessageModel object.
    public MessageModel parseMessage(String command) {
        if (!command.startsWith(COMMAND_SEND_MSG + DELIMITER)) {
            return null;
        }

        // We limit the split to 7 parts. This is a neat trick to ensure that if the
        // message content itself contains "::", it won't be split further.
        String[] parts = command.split(DELIMITER, 7); 
        if (parts.length < 7) {
            return null;
        }

        try {
            UUID messageId = UUID.fromString(parts[1]);
            LocalDateTime timestamp = LocalDateTime.parse(parts[2]);
            String type = parts[3];
            String sender = parts[4];
            UUID chatroomId = UUID.fromString(parts[5]);
            String content = parts[6];
            
            return new MessageModel(messageId, timestamp, type, sender, chatroomId, content);
        } catch (IllegalArgumentException e) {
            // This will catch any parsing errors, e.g., if a UUID is malformed.
            System.err.println("Error parsing message: " + command);
            return null;
        }
    }
}