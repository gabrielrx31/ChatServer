package server.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import Services.ChatroomManager;
import Services.ClientManager;
import Services.DataTransferManager;
import Services.LoginService;
import common.models.ChatRoom;
import common.models.Datahandler;
import common.models.MessageModel;
import common.models.User;
import common.protocols.MessageProtocol;
import server.utils.Logger;


// Each instance of this class handles all communication for a single connected client.
public class ServerHandler implements Runnable {
    // A nested class to temporarily hold details about a pending file transfer.
    private static class PendingTransfer {
        UUID senderClientId;
        String recipientUsername;
        long fileSize;
        public PendingTransfer(UUID sender, String recipient, long size) {
            this.senderClientId = sender; this.recipientUsername = recipient; this.fileSize = size;
        }
    }
    // A static map to track all active file transfer negotiations across all handlers.
    private static final Map<UUID, PendingTransfer> pendingFileTransfers = new ConcurrentHashMap<>();

    private final Socket controlSocket;
    private final UUID clientId;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private boolean isAuthenticated = false;
    private LoginService loginService;
    private User currentUser;
    private MessageProtocol messageProtocol;
    private final Datahandler datahandler;
    private UUID currentChatroomId;

    public ServerHandler(Socket socket, LoginService loginService, Datahandler datahandler) {
        this.controlSocket = socket;
        this.clientId = UUID.randomUUID(); // Assign a unique ID to this client's session.
        this.loginService = loginService;
        this.messageProtocol = new MessageProtocol();
        this.datahandler = datahandler;
        this.currentChatroomId = null;
    }

    @Override
    public void run() {
        try {
            ClientManager.addClient(clientId, controlSocket);
            dataIn = new DataInputStream(controlSocket.getInputStream());
            dataOut = new DataOutputStream(controlSocket.getOutputStream());
            // The main loop for this client's session. It continuously reads messages.
            while (true) {
                String clientMessage = dataIn.readUTF();
                handleClientMessage(clientMessage);
            }
        } catch (IOException e) {
            // This block is typically reached when a client disconnects unexpectedly.
            System.err.println("Control connection to client " + clientId + " lost: " + e.getMessage());
        } finally {
            // Ensures cleanup happens regardless of how the connection ends.
            closeConnection();
        }
    }

    // Acts as a command router, parsing the initial command from the client's message.
    private void handleClientMessage(String message) {
        try {
            String[] parts = message.split("::", 2);
            String command = parts[0];
            // Authentication checks are performed before executing commands.
            switch (command) {
                case "LOGIN": handleLogin(message); break;
                case "LOGOUT": handleLogout(); break;
                case "LIST_ROOMS": sendChatroomList(); break;
                case "JOIN_ROOM": if (isAuthenticated) handleJoinRoom(message); break;
                case "SEND_MSG": if (isAuthenticated) handleTextMessage(message); break;
                
                case "WANT_TO_SEND_FILE": if (isAuthenticated) handleFileTransferRequest(message); break;
                case "ACCEPT_FILE": if (isAuthenticated) handleFileResponse(message, true); break;
                case "REJECT_FILE": if (isAuthenticated) handleFileResponse(message, false); break;

                default: dataOut.writeUTF("Invalid command: " + command); break;
            }
        } catch (IOException e) {
            System.err.println("Error handling client message: " + e.getMessage());
        }
    }

    // Handles the first step of a file transfer: the sender's request.
    private void handleFileTransferRequest(String message) throws IOException {
        String[] parts = message.split("::", 4);
        if (parts.length < 4) return;
        String recipientUsername = parts[1];
        String filename = parts[2];
        long fileSize = Long.parseLong(parts[3]);

        User recipientUser = loginService.getUserByUsername(recipientUsername);
        if (recipientUser == null || !loginService.isUserActive(recipientUser.getId())) {
            dataOut.writeUTF("INFO::User '" + recipientUsername + "' is not online.");
            return;
        }

        UUID transferId = UUID.randomUUID();
        pendingFileTransfers.put(transferId, new PendingTransfer(this.clientId, recipientUsername, fileSize));

        // Forward the file transfer request to the recipient.
        String recipientClientIdStr = loginService.getClientId(recipientUser.getId());
        UUID recipientClientId = UUID.fromString(recipientClientIdStr);
        String forwardMessage = "INCOMING_FILE::" + currentUser.getUserName() + "::" + filename + "::" + fileSize + "::" + transferId;
        ClientManager.unibroadcastMessage(recipientClientId, forwardMessage);
        dataOut.writeUTF("INFO::File transfer request sent to " + recipientUsername + ". Waiting for response...");
    }

    // Handles the second step: the recipient's response (accept or reject).
    private void handleFileResponse(String message, boolean accepted) {
        String[] parts = message.split("::", 2);
        if (parts.length < 2) return;
        UUID transferId = UUID.fromString(parts[1]);

        PendingTransfer transfer = pendingFileTransfers.get(transferId);
        if (transfer == null) return; // The transfer request may have expired or been invalid.

        if (accepted) {
            DataTransferManager.registerTransferSize(transferId, transfer.fileSize);
            // Instruct both clients to connect to the data port with the same transfer ID.
            String senderMessage = "START_FILE_TRANSFER::5011::" + transferId.toString();
            ClientManager.unibroadcastMessage(transfer.senderClientId, senderMessage);
            String receiverMessage = "PROCEED_WITH_DOWNLOAD::5011::" + transferId.toString();
            ClientManager.unibroadcastMessage(this.clientId, receiverMessage);
        } else {
            // Inform the original sender that the request was rejected.
            String senderMessage = "REJECT_FILE_TRANSFER::" + currentUser.getUserName();
            ClientManager.unibroadcastMessage(transfer.senderClientId, senderMessage);
        }
        pendingFileTransfers.remove(transferId); // Clean up the pending transfer.
    }

    private void handleLogin(String message) throws IOException {
        String[] parts = message.split("::", 2);
        if (parts.length < 2) { dataOut.writeUTF("ERROR: Invalid login format."); return; }
        String username = parts[1];
        // The server constructs the formal login message to be parsed by the LoginService.
        String loginMessage = clientId.toString() + "|" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "|LOGIN|" + username;
        currentUser = loginService.parseAndValidateLogin(loginMessage);
        if (currentUser != null) {
            this.isAuthenticated = true;
            dataOut.writeUTF("SUCCESS: Logged in as " + currentUser.getUserName());
            Logger.info(Logger.LogEvent.USER_SESSION, "Client logget ind: " + currentUser.getUserName());
        } else {
            this.isAuthenticated = false;
            dataOut.writeUTF("ERROR: Login failed.");
        }
    }

    private void handleLogout() throws IOException {
        if (isAuthenticated) {
            loginService.logoutByClientId(clientId.toString());
            this.isAuthenticated = false;
            dataOut.writeUTF("SUCCESS: Logged out.");
            Logger.info(Logger.LogEvent.USER_SESSION, "Client logged out: " + (currentUser != null ? currentUser.getUserName() : "Unknown"));
        } else {
            dataOut.writeUTF("ERROR: Not logged in.");
        }
    }

    // Manages a user's transition between chat rooms.
    private void handleJoinRoom(String message) throws IOException {
        String[] parts = message.split("::", 2);
        if (parts.length < 2) { dataOut.writeUTF("ERROR: Invalid room ID format."); return; }
        try {
            UUID newChatroomId = UUID.fromString(parts[1]);
            ChatRoom newRoom = ChatroomManager.getChatroomById(newChatroomId);
            if (newRoom != null && currentUser != null) {
                // If the user is already in a room, notify others of their departure.
                if (this.currentChatroomId != null && !this.currentChatroomId.equals(newChatroomId)) {
                    ChatRoom oldRoom = ChatroomManager.getChatroomById(this.currentChatroomId);
                    if (oldRoom != null) {
                        oldRoom.removeMember(currentUser.getId());
                        String leaveNotification = "[SYSTEM]: " + currentUser.getUserName() + " has left the room.";
                        for (UUID memberId : oldRoom.getMembers()) {
                            String clientSessionIdStr = loginService.getClientId(memberId);
                            if (clientSessionIdStr != null) ClientManager.unibroadcastMessage(UUID.fromString(clientSessionIdStr), leaveNotification);
                        }
                    }
                }
                this.currentChatroomId = newChatroomId;
                newRoom.addMember(currentUser.getId());
                // Notify members of the new room about the user's arrival.
                String joinNotification = "[SYSTEM]: " + currentUser.getUserName() + " has joined the room.";
                for (UUID memberId : newRoom.getMembers()) {
                    if (!memberId.equals(currentUser.getId())) {
                        String clientSessionIdStr = loginService.getClientId(memberId);
                        if (clientSessionIdStr != null) ClientManager.unibroadcastMessage(UUID.fromString(clientSessionIdStr), joinNotification);
                    }
                }
                // Send the new room's message history to the user.
                dataOut.writeUTF("--- Displaying history for " + newRoom.getRoomName() + " ---");
                List<MessageModel> history = datahandler.getMessages().stream().filter(msg -> msg.chatroomId.equals(newChatroomId)).collect(Collectors.toList());
                if (history.isEmpty()) { dataOut.writeUTF("... No messages in this room yet ..."); }
                else { for (MessageModel msg : history) { dataOut.writeUTF("[" + msg.sender + "]: " + msg.content); } }
                dataOut.writeUTF("--- End of history ---");
            } else { dataOut.writeUTF("ERROR: Could not find the room."); }
        } catch (IllegalArgumentException e) { dataOut.writeUTF("ERROR: Invalid UUID format for room ID."); }
    }

    // Handles an incoming text message from the client.
    private void handleTextMessage(String message) {
        MessageModel parsedMessage = messageProtocol.parseMessage(message);
        if (parsedMessage != null) {
            // Converts any emoji shortcuts (e.g., ":)") into Unicode characters.
            parsedMessage.content = EmojiConverter.convert(parsedMessage.content);

            this.datahandler.addMessageToArray(parsedMessage);
            ChatRoom targetRoom = ChatroomManager.getChatroomById(parsedMessage.chatroomId);
            if (targetRoom != null) {
                String formattedMessage = "[" + targetRoom.getRoomName() + " | " + parsedMessage.sender + "]: " + parsedMessage.content;
                // Broadcast the message to all members of the target chat room.
                for (UUID memberId : targetRoom.getMembers()) {
                    String clientSessionIdStr = loginService.getClientId(memberId);
                    if (clientSessionIdStr != null) ClientManager.unibroadcastMessage(UUID.fromString(clientSessionIdStr), formattedMessage);
                }
            }
        }
    }

    // Sends the list of available chat rooms to the client.
    private void sendChatroomList() throws IOException {
        dataOut.writeUTF("START_CHATROOM_LIST");
        for (ChatRoom room : ChatroomManager.getAllChatrooms().values()) {
            dataOut.writeUTF(room.getId().toString() + "::" + room.getRoomName());
        }
        dataOut.writeUTF("END_CHATROOM_LIST");
    }
    
    // Cleans up all resources associated with this client's session.
    private void closeConnection() {
        // Cancel any pending file transfers initiated by this client.
        pendingFileTransfers.entrySet().removeIf(entry -> entry.getValue().senderClientId.equals(this.clientId));
        
        // Notify the current chat room that the user has left.
        if (this.currentChatroomId != null && currentUser != null) {
            ChatRoom oldRoom = ChatroomManager.getChatroomById(this.currentChatroomId);
            if (oldRoom != null) {
                oldRoom.removeMember(currentUser.getId());
                String leaveNotification = "[SYSTEM]: " + currentUser.getUserName() + " has left the room.";
                for (UUID memberId : oldRoom.getMembers()) {
                    String clientSessionIdStr = loginService.getClientId(memberId);
                    if (clientSessionIdStr != null) ClientManager.unibroadcastMessage(UUID.fromString(clientSessionIdStr), leaveNotification);
                }
            }
        }
        // Log the user out and remove them from active managers.
        if (currentUser != null) loginService.logoutByClientId(clientId.toString());
        ClientManager.removeClient(clientId);
        try { if (controlSocket != null) controlSocket.close(); } catch (IOException e) { /* Ignored */ }
        System.out.println("Connection closed for client: " + clientId);
    }
}