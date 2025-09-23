package common.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class MessageModel {
    public UUID messageId;
    public LocalDateTime timestamp;
    public String type;
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