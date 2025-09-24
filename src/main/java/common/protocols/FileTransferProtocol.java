package common.protocols;

import java.util.UUID;

// Defines the structure for creating and parsing file transfer commands.
// This class seems deprecated or is an alternative version, as the main logic
// uses a different set of commands like "WANT_TO_SEND_FILE".
public class FileTransferProtocol {
    public static final String COMMAND_SEND_FILE = "SEND_FILE";
    private static final String DELIMITER = "::";

    // Assembles a command string for a file transfer.
    public String buildFileCommand(String sender, UUID chatroomId, String fileName, long fileSize) {
        long timestamp = System.currentTimeMillis();
        return COMMAND_SEND_FILE + DELIMITER + timestamp + DELIMITER + sender + DELIMITER + chatroomId.toString() + DELIMITER + fileName + DELIMITER + fileSize;
    } 

    // Deconstructs a command string into a FileTransferMessage object.
    public FileTransferMessage parseFileCommand(String command) {
        if (!command.startsWith(COMMAND_SEND_FILE + DELIMITER)) {
            return null;
        }

        String[] parts = command.split(DELIMITER);
        if (parts.length < 6) {
            return null; 
        }

        try {
            long timestamp = Long.parseLong(parts[1]);
            String sender = parts[2];
            UUID chatroomId = UUID.fromString(parts[3]);
            String fileName = parts[4];
            long fileSize = Long.parseLong(parts[5]);
            
            return new FileTransferMessage(sender, chatroomId, fileName, fileSize, timestamp);
        } catch (IllegalArgumentException e) {
            System.out.println("Error parsing file command: " + e.getMessage());
            return null; 
        }
    }
}