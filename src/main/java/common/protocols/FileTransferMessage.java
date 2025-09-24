package common.protocols;

import java.util.UUID;

// A simple, immutable data structure to hold information about a file transfer.
// Using 'final' fields makes instances of this class thread-safe by default.
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