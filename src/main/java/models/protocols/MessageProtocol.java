package models.protocols;

import java.time.LocalDateTime;
import java.util.UUID;

import models.MessageModel;

public class MessageProtocol {
    public static final String COMMAND_SEND_MSG = "SEND_MSG";
    public static final String MSG_TYPE_TEXT = "TEXT";
    public static final String MSG_TYPE_SYSTEM = "SYSTEM";
    private static final String DELIMITER = "::";

    public String buildTextMessage(String sender, UUID chatroomId, String content) {
        UUID messageId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.now();
        return COMMAND_SEND_MSG + DELIMITER + messageId.toString() + DELIMITER + timestamp.toString() + DELIMITER + MSG_TYPE_TEXT + DELIMITER + sender + DELIMITER + chatroomId.toString() + DELIMITER + content;
    }

    public String buildSystemMessage(String content, UUID chatroomId) {
        UUID messageId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.now();
        return COMMAND_SEND_MSG + DELIMITER + messageId.toString() + DELIMITER + timestamp.toString() + DELIMITER + MSG_TYPE_SYSTEM + DELIMITER + "SYSTEM" + DELIMITER + chatroomId.toString() + DELIMITER + content;
    }
    
    public MessageModel parseMessage(String command) {
        if (!command.startsWith(COMMAND_SEND_MSG + DELIMITER)) {
            return null;
        }

        String[] parts = command.split(DELIMITER);
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
            return null;
        }
    }
}