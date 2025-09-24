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


public class ServerHandler implements Runnable {
    private static class PendingTransfer {
        UUID senderClientId;
        String recipientUsername;
        long fileSize;
        public PendingTransfer(UUID sender, String recipient, long size) {
            this.senderClientId = sender; this.recipientUsername = recipient; this.fileSize = size;
        }
    }
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
        this.clientId = UUID.randomUUID();
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
            while (true) {
                String clientMessage = dataIn.readUTF();
                handleClientMessage(clientMessage);
            }
        } catch (IOException e) {
            System.err.println("Kontrol-forbindelse til klient " + clientId + " mistet: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void handleClientMessage(String message) {
        try {
            String[] parts = message.split("::", 2);
            String command = parts[0];
            switch (command) {
                case "LOGIN": handleLogin(message); break;
                case "LOGOUT": handleLogout(); break;
                case "LIST_ROOMS": sendChatroomList(); break;
                case "JOIN_ROOM": if (isAuthenticated) handleJoinRoom(message); break;
                case "SEND_MSG": if (isAuthenticated) handleTextMessage(message); break;
                
                case "WANT_TO_SEND_FILE": if (isAuthenticated) handleFileTransferRequest(message); break;
                case "ACCEPT_FILE": if (isAuthenticated) handleFileResponse(message, true); break;
                case "REJECT_FILE": if (isAuthenticated) handleFileResponse(message, false); break;

                default: dataOut.writeUTF("Ugyldig kommando: " + command); break;
            }
        } catch (IOException e) {
            System.err.println("Fejl under håndtering af klientbesked: " + e.getMessage());
        }
    }

    private void handleFileTransferRequest(String message) throws IOException {
        String[] parts = message.split("::", 4);
        if (parts.length < 4) return;
        String recipientUsername = parts[1];
        String filename = parts[2];
        long fileSize = Long.parseLong(parts[3]);

        User recipientUser = loginService.getUserByUsername(recipientUsername);
        if (recipientUser == null || !loginService.isUserActive(recipientUser.getId())) {
            dataOut.writeUTF("INFO::Brugeren '" + recipientUsername + "' er ikke online.");
            return;
        }

        UUID transferId = UUID.randomUUID();
        pendingFileTransfers.put(transferId, new PendingTransfer(this.clientId, recipientUsername, fileSize));

        String recipientClientIdStr = loginService.getClientId(recipientUser.getId());
        UUID recipientClientId = UUID.fromString(recipientClientIdStr);
        String forwardMessage = "INCOMING_FILE::" + currentUser.getUserName() + "::" + filename + "::" + fileSize + "::" + transferId;
        ClientManager.unibroadcastMessage(recipientClientId, forwardMessage);
        dataOut.writeUTF("INFO::Anmodning om filoverførsel sendt til " + recipientUsername + ". Venter på svar...");
    }

    private void handleFileResponse(String message, boolean accepted) {
        String[] parts = message.split("::", 2);
        if (parts.length < 2) return;
        UUID transferId = UUID.fromString(parts[1]);

        PendingTransfer transfer = pendingFileTransfers.get(transferId);
        if (transfer == null) return;

        if (accepted) {
            DataTransferManager.registerTransferSize(transferId, transfer.fileSize);
            String senderMessage = "START_FILE_TRANSFER::5011::" + transferId.toString();
            ClientManager.unibroadcastMessage(transfer.senderClientId, senderMessage);
            String receiverMessage = "PROCEED_WITH_DOWNLOAD::5011::" + transferId.toString();
            ClientManager.unibroadcastMessage(this.clientId, receiverMessage);
        } else {
            String senderMessage = "REJECT_FILE_TRANSFER::" + currentUser.getUserName();
            ClientManager.unibroadcastMessage(transfer.senderClientId, senderMessage);
        }
        pendingFileTransfers.remove(transferId);
    }

    private void handleLogin(String message) throws IOException {
        String[] parts = message.split("::", 2);
        if (parts.length < 2) { dataOut.writeUTF("FEJL: Ugyldigt loginformat."); return; }
        String username = parts[1];
        String loginMessage = clientId.toString() + "|" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "|LOGIN|" + username;
        currentUser = loginService.parseAndValidateLogin(loginMessage);
        if (currentUser != null) {
            this.isAuthenticated = true;
            dataOut.writeUTF("SUCCESS: Logget ind som " + currentUser.getUserName());
        } else {
            this.isAuthenticated = false;
            dataOut.writeUTF("FEJL: Login mislykkedes.");
        }
    }

    private void handleLogout() throws IOException {
        if (isAuthenticated) {
            loginService.logoutByClientId(clientId.toString());
            this.isAuthenticated = false;
            dataOut.writeUTF("SUCCESS: Logget ud.");
        } else {
            dataOut.writeUTF("FEJL: Ikke logget ind.");
        }
    }

    private void handleJoinRoom(String message) throws IOException {
        String[] parts = message.split("::", 2);
        if (parts.length < 2) { dataOut.writeUTF("FEJL: Ugyldigt rum-ID format."); return; }
        try {
            UUID newChatroomId = UUID.fromString(parts[1]);
            ChatRoom newRoom = ChatroomManager.getChatroomById(newChatroomId);
            if (newRoom != null && currentUser != null) {
                if (this.currentChatroomId != null && !this.currentChatroomId.equals(newChatroomId)) {
                    ChatRoom oldRoom = ChatroomManager.getChatroomById(this.currentChatroomId);
                    if (oldRoom != null) {
                        oldRoom.removeMember(currentUser.getId());
                        String leaveNotification = "[SYSTEM]: " + currentUser.getUserName() + " har forladt rummet.";
                        for (UUID memberId : oldRoom.getMembers()) {
                            String clientSessionIdStr = loginService.getClientId(memberId);
                            if (clientSessionIdStr != null) ClientManager.unibroadcastMessage(UUID.fromString(clientSessionIdStr), leaveNotification);
                        }
                    }
                }
                this.currentChatroomId = newChatroomId;
                newRoom.addMember(currentUser.getId());
                String joinNotification = "[SYSTEM]: " + currentUser.getUserName() + " har joinet rummet.";
                for (UUID memberId : newRoom.getMembers()) {
                    if (!memberId.equals(currentUser.getId())) {
                        String clientSessionIdStr = loginService.getClientId(memberId);
                        if (clientSessionIdStr != null) ClientManager.unibroadcastMessage(UUID.fromString(clientSessionIdStr), joinNotification);
                    }
                }
                dataOut.writeUTF("--- Viser historik for " + newRoom.getRoomName() + " ---");
                List<MessageModel> history = datahandler.getMessages().stream().filter(msg -> msg.chatroomId.equals(newChatroomId)).collect(Collectors.toList());
                if (history.isEmpty()) { dataOut.writeUTF("... Ingen beskeder i dette rum endnu ..."); }
                else { for (MessageModel msg : history) { dataOut.writeUTF("[" + msg.sender + "]: " + msg.content); } }
                dataOut.writeUTF("--- Slut på historik ---");
            } else { dataOut.writeUTF("FEJL: Kunne ikke finde rummet."); }
        } catch (IllegalArgumentException e) { dataOut.writeUTF("FEJL: Ugyldigt UUID format for rum-ID."); }
    }

    private void handleTextMessage(String message) {
        MessageModel parsedMessage = messageProtocol.parseMessage(message);
        if (parsedMessage != null) {
            this.datahandler.addMessageToArray(parsedMessage);
            ChatRoom targetRoom = ChatroomManager.getChatroomById(parsedMessage.chatroomId);
            if (targetRoom != null) {
                String formattedMessage = "[" + targetRoom.getRoomName() + " | " + parsedMessage.sender + "]: " + parsedMessage.content;
                for (UUID memberId : targetRoom.getMembers()) {
                    String clientSessionIdStr = loginService.getClientId(memberId);
                    if (clientSessionIdStr != null) ClientManager.unibroadcastMessage(UUID.fromString(clientSessionIdStr), formattedMessage);
                }
            }
        }
    }

    private void sendChatroomList() throws IOException {
        dataOut.writeUTF("START_CHATROOM_LIST");
        for (ChatRoom room : ChatroomManager.getAllChatrooms().values()) {
            dataOut.writeUTF(room.getId().toString() + "::" + room.getRoomName());
        }
        dataOut.writeUTF("END_CHATROOM_LIST");
    }
    
    private void closeConnection() {
        pendingFileTransfers.entrySet().removeIf(entry -> entry.getValue().senderClientId.equals(this.clientId));
        if (this.currentChatroomId != null && currentUser != null) {
            ChatRoom oldRoom = ChatroomManager.getChatroomById(this.currentChatroomId);
            if (oldRoom != null) {
                oldRoom.removeMember(currentUser.getId());
                String leaveNotification = "[SYSTEM]: " + currentUser.getUserName() + " har forladt rummet.";
                for (UUID memberId : oldRoom.getMembers()) {
                    String clientSessionIdStr = loginService.getClientId(memberId);
                    if (clientSessionIdStr != null) ClientManager.unibroadcastMessage(UUID.fromString(clientSessionIdStr), leaveNotification);
                }
            }
        }
        if (currentUser != null) loginService.logoutByClientId(clientId.toString());
        ClientManager.removeClient(clientId);
        try { if (controlSocket != null) controlSocket.close(); } catch (IOException e) { /* Ignored */ }
        System.out.println("Forbindelse lukket for klient: " + clientId);
    }
}