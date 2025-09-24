package common.models;

import java.time.LocalDateTime;
import java.util.UUID;

// A simple data object that represents a single message in the system.
public class MessageModel {
    public UUID messageId;
    public LocalDateTime timestamp;
    public String type; // e.g., "TEXT" or "SYSTEM"
    public String sender;
    public UUID chatroomId;
    public String content;

    public MessageModel(UUID messageId, LocalDateTime timestamp, String type, String sender, UUID chatroomId, String content) {
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.type = type;
        this.sender = sender;
        this.chatroomId = chatroomId;
        this.content = content;
    }
}