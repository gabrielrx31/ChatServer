package models;

import java.util.UUID;

public class MessageModel {
    public final long timestamp;
    public final String type;
    public final String sender;
    public final UUID chatroomId;
    public final String content;

    public MessageModel(long timestamp, String type, String sender, UUID chatroomId, String content) {
        this.timestamp = timestamp;
        this.type = type;
        this.sender = sender;
        this.chatroomId = chatroomId;
        this.content = content;
    }
}