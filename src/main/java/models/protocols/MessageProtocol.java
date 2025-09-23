package models.protocols;

import java.util.UUID;

import models.MessageModel;

public class MessageProtocol {
    public static final String COMMAND_SEND_MSG = "SEND_MSG";
    public static final String MSG_TYPE_TEXT = "TEXT";
    public static final String MSG_TYPE_SYSTEM = "SYSTEM";
    private static final String DELIMITER = "::";

    public String buildTextMessage(String sender, UUID chatroomId, String content) {
        long timestamp = System.currentTimeMillis();
        return COMMAND_SEND_MSG + DELIMITER + timestamp + DELIMITER + MSG_TYPE_TEXT + DELIMITER + sender + DELIMITER + chatroomId.toString() + DELIMITER + content;
    }

    public String buildSystemMessage(String content, UUID chatroomId) {
        long timestamp = System.currentTimeMillis();
        return COMMAND_SEND_MSG + DELIMITER + timestamp + DELIMITER + MSG_TYPE_SYSTEM + DELIMITER + "SYSTEM" + DELIMITER + chatroomId.toString() + DELIMITER + content;
    }
    
    public MessageModel parseMessage(String command) {
        if (!command.startsWith(COMMAND_SEND_MSG + DELIMITER)) {
            return null;
        }

        String[] parts = command.split(DELIMITER);
        if (parts.length < 6) {
            return null;
        }

        try {
            long timestamp = Long.parseLong(parts[1]);
            String type = parts[2];
            String sender = parts[3];
            UUID chatroomId = UUID.fromString(parts[4]);
            String content = parts[5];
            
            return new MessageModel(timestamp, type, sender, chatroomId, content);
        } catch (IllegalArgumentException | NumberFormatException e) {
            return null;
        }
    }
}