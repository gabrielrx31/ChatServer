package models.protocols;

import java.util.UUID;

public class FileTransferMessage {
    public final String sender;
    public final UUID chatroomId;
    public final String fileName;
    public final long fileSize;
    public final long timestamp;

    public FileTransferMessage(String sender, UUID chatroomId, String fileName, long fileSize, long timestamp) {
        this.sender = sender;
        this.chatroomId = chatroomId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.timestamp = timestamp;
    }
}