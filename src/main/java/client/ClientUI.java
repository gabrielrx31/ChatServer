package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import common.protocols.MessageProtocol;

public class ClientUI {
    // Defines what the client is currently waiting for from the user.
    private enum ClientState {
        AWAITING_MENU_CHOICE, AWAITING_CHATROOM_CHOICE, AWAITING_MESSAGE,
        AWAITING_SYSTEM_MESSAGE, AWAITING_FILE_ACCEPT
    }

    private static final String SERVER_ADDRESS = "localhost";
    private final int controlPort = 5010;

    // Network streams for the main command channel.
    private Socket controlSocket;
    private DataOutputStream controlOut;
    private DataInputStream controlIn;
    
    private Scanner scanner;
    private UUID chatroomId;
    private boolean isLoggedIn = false;
    private String username;
    // Caches available chatrooms locally to avoid requesting them repeatedly.
    private Map<String, UUID> chatroomMap = new HashMap<>();
    private volatile ClientState clientState = ClientState.AWAITING_MENU_CHOICE;

    // Holds the state for a single file transfer at a time.
    private UUID fileTransferId = null;
    private String fileToSendPath = null;
    private String fileToSendRecipient = null;
    private String incomingFilename = null;
    private long incomingFileSize = 0;

    public ClientUI() {
        try {
            this.controlSocket = new Socket(SERVER_ADDRESS, controlPort);
            this.controlOut = new DataOutputStream(controlSocket.getOutputStream());
            this.controlIn = new DataInputStream(controlSocket.getInputStream());
            this.scanner = new Scanner(System.in);
            
            System.out.println("Connection established to server on port " + controlPort);
            
            // A separate thread is crucial for listening to the server
            // without blocking the main thread that reads user input.
            new Thread(new ServerListener()).start();
            runClientUI();
        } catch (IOException e) {
            System.err.println("Error: Could not connect to the server. " + e.getMessage());
        }
    }
    
    // The main loop for handling user input. It delegates actions based on the current state.
    private void runClientUI() {
        showLoginMenu();
        try {
            while (true) {
                String input = scanner.nextLine();
                if (!isLoggedIn) {
                    this.username = input;
                    controlOut.writeUTF("LOGIN::" + this.username);
                } else {
                    switch (clientState) {
                        case AWAITING_MENU_CHOICE: handleMenuChoice(input); break;
                        case AWAITING_CHATROOM_CHOICE: handleChatroomChoice(input); break;
                        case AWAITING_MESSAGE: handleMessageInput(input); break;
                        case AWAITING_SYSTEM_MESSAGE: handleSystemMessageInput(input); break;
                        case AWAITING_FILE_ACCEPT: handleFileAccept(input); break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error sending command: " + e.getMessage());
        }
    }

    private void showLoginMenu() {
        System.out.println("--- Login Menu ---");
        System.out.print("Enter your username: ");
    }
    
    private void showMainMenu() {
        System.out.println("\n--- Main Menu ---\n1. Send message to chatroom\n2. Change chatroom\n3. Send system message\n4. Logout\n5. Send a file");
        System.out.print("Select an action: ");
        clientState = ClientState.AWAITING_MENU_CHOICE;
    }

    private void handleMenuChoice(String choice) throws IOException {
        switch (choice) {
            case "1": sendMessageToChatroom(); break;
            case "2": changeChatroom(); break;
            case "3": sendSystemMessage(); break;
            case "4": logout(); break;
            case "5": initiateFileTransfer(); break;
            default: System.out.println("Invalid choice. Please try again."); showMainMenu(); break;
        }
    }

    // Joins a chatroom based on user input, using the cached chatroomMap.
    private void handleChatroomChoice(String roomName) throws IOException {
        String lowerCaseRoomName = roomName.toLowerCase();
        if (chatroomMap.containsKey(lowerCaseRoomName)) {
            this.chatroomId = chatroomMap.get(lowerCaseRoomName);
            controlOut.writeUTF("JOIN_ROOM::" + this.chatroomId.toString());
            System.out.println("Joining chatroom: " + roomName);
        } else {
            System.out.println("Invalid chatroom name. Please try again.");
        }
        showMainMenu();
    }
    
    private void handleMessageInput(String messageContent) throws IOException {
        MessageProtocol msgProtocol = new MessageProtocol();
        String message = msgProtocol.buildTextMessage(username, chatroomId, messageContent);
        controlOut.writeUTF(message);
        showMainMenu();
    }

    private void handleSystemMessageInput(String messageContent) throws IOException {
        MessageProtocol msgProtocol = new MessageProtocol();
        String message = msgProtocol.buildSystemMessage(messageContent, chatroomId);
        controlOut.writeUTF(message);
        showMainMenu();
    }

    private void sendMessageToChatroom() throws IOException {
        if (chatroomId == null) { System.out.println("Select a chatroom before sending a message."); changeChatroom(); }
        else { System.out.print("Enter your message: "); clientState = ClientState.AWAITING_MESSAGE; }
    }

    private void sendSystemMessage() throws IOException {
        if (chatroomId == null) { System.out.println("Select a chatroom before sending a message."); changeChatroom(); }
        else { System.out.print("Enter your system message: "); clientState = ClientState.AWAITING_SYSTEM_MESSAGE; }
    }

    private void changeChatroom() throws IOException { System.out.println("Fetching available chatrooms..."); controlOut.writeUTF("LIST_ROOMS"); }
    
    // Gathers file info and sends a request to the server to initiate a transfer.
    private void initiateFileTransfer() throws IOException {
        System.out.print("Enter the recipient's username: ");
        this.fileToSendRecipient = scanner.nextLine();
        System.out.print("Enter the name of the file in your 'uploads' folder: ");
        String filename = scanner.nextLine();
        this.fileToSendPath = "uploads/" + filename;
        File file = new File(fileToSendPath);
        if (file.exists() && !file.isDirectory()) {
            long fileSize = file.length();
            controlOut.writeUTF("WANT_TO_SEND_FILE::" + fileToSendRecipient + "::" + filename + "::" + fileSize);
        } else {
            System.out.println("Error: The file '" + filename + "' does not exist in your 'uploads' folder.");
            showMainMenu();
        }
    }

    // Handles the user's 'ja' or 'nej' response to a file request.
    private void handleFileAccept(String answer) throws IOException {
        if (answer.equalsIgnoreCase("ja")) {
            controlOut.writeUTF("ACCEPT_FILE::" + this.fileTransferId);
            System.out.println("Accepted file transfer. Waiting for instructions from server...");
        } else {
            controlOut.writeUTF("REJECT_FILE::" + this.fileTransferId);
            System.out.println("Rejected file transfer.");
        }
        this.fileTransferId = null; // Reset transfer state.
        showMainMenu();
    }

    // Connects to the server's data port to stream the file.
    private void sendFile(int dataPort, UUID transferId) {
        try (Socket dataSocket = new Socket(SERVER_ADDRESS, dataPort);
             DataOutputStream dataOutStream = new DataOutputStream(dataSocket.getOutputStream());
             FileInputStream fis = new FileInputStream(this.fileToSendPath)) {
            
            System.out.println("Connecting to data port " + dataPort + " to send file...");
            dataOutStream.writeUTF(transferId.toString()); // Sends the ID to pair with the receiver.

            File file = new File(this.fileToSendPath);
            byte[] buffer = new byte[8192];
            int bytesRead;
            System.out.println("Starting file transmission: " + file.getName());
            while ((bytesRead = fis.read(buffer)) != -1) {
                dataOutStream.write(buffer, 0, bytesRead);
            }
            dataOutStream.flush();
            System.out.println("File sent successfully. Closing data connection.");

        } catch (IOException e) {
            System.err.println("Error during file transmission: " + e.getMessage());
        } finally {
            this.fileToSendPath = null;
            this.fileToSendRecipient = null;
            showMainMenu();
        }
    }

    // Connects to the server's data port to download the file.
    private void receiveFile(int dataPort, UUID transferId) {
        try (Socket dataSocket = new Socket(SERVER_ADDRESS, dataPort);
             DataOutputStream dataOutStream = new DataOutputStream(dataSocket.getOutputStream());
             DataInputStream dataInStream = new DataInputStream(dataSocket.getInputStream());
             FileOutputStream fos = new FileOutputStream("Downloads/" + this.incomingFilename)) {

            System.out.println("Connecting to data port " + dataPort + " to receive file...");
            dataOutStream.writeUTF(transferId.toString()); // Sends the ID to pair with the sender.

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesReceived = 0;
            System.out.println("Receiving file: " + this.incomingFilename);
            // Reads from the socket until the total file size is reached.
            while (totalBytesReceived < this.incomingFileSize && (bytesRead = dataInStream.read(buffer, 0, (int)Math.min(buffer.length, this.incomingFileSize - totalBytesReceived))) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytesReceived += bytesRead;
            }
            System.out.println("File received successfully and saved in Downloads folder. Closing data connection.");

        } catch (IOException e) {
            System.err.println("Error while receiving file: " + e.getMessage());
        } finally {
            this.incomingFilename = null;
            this.incomingFileSize = 0;
            showMainMenu();
        }
    }

    private void logout() throws IOException { controlOut.writeUTF("LOGOUT"); }

    private void closeConnection() {
        try { if (controlSocket != null) controlSocket.close(); } catch (IOException e) { /* Ignored */ }
        System.exit(0);
    }

    // Handles all incoming communication from the server in the background.
    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    String serverMessage = controlIn.readUTF();
                    
                    if (serverMessage.startsWith("INCOMING_FILE::")) {
                        String[] parts = serverMessage.split("::", 5);
                        String fromUser = parts[1];
                        incomingFilename = parts[2];
                        incomingFileSize = Long.parseLong(parts[3]);
                        fileTransferId = UUID.fromString(parts[4]);
                        System.out.println("\n[File Transfer] " + fromUser + " wants to send you: " + incomingFilename + " (" + incomingFileSize + " bytes).");
                        System.out.print("Accept? (ja/nej): ");
                        clientState = ClientState.AWAITING_FILE_ACCEPT;
                    } 
                    else if (serverMessage.startsWith("START_FILE_TRANSFER::")) {
                        String[] parts = serverMessage.split("::", 3);
                        int dataPort = Integer.parseInt(parts[1]);
                        UUID transferId = UUID.fromString(parts[2]);
                        System.out.println("\nINFO: Recipient accepted. Preparing to send...");
                        new Thread(() -> sendFile(dataPort, transferId)).start();
                    } 
                    else if (serverMessage.startsWith("PROCEED_WITH_DOWNLOAD::")) {
                        String[] parts = serverMessage.split("::", 3);
                        int dataPort = Integer.parseInt(parts[1]);
                        UUID transferId = UUID.fromString(parts[2]);
                        System.out.println("\nINFO: Server is ready. Starting download...");
                        new Thread(() -> receiveFile(dataPort, transferId)).start();
                    }
                    else if (serverMessage.startsWith("REJECT_FILE_TRANSFER::")) {
                        System.out.println("\nINFO: " + serverMessage.split("::", 2)[1] + " rejected the file transfer.");
                        showMainMenu();
                    }
                    else {
                        handleStandardMessages(serverMessage);
                    }
                }
            } catch (IOException e) {
                System.out.println("\nConnection to the server has been lost.");
            } finally {
                closeConnection();
            }
        }
    }
    
    // Directs standard server messages (login status, chat, etc.) to the correct handler.
    private void handleStandardMessages(String serverMessage) throws IOException {
        if (serverMessage.startsWith("SUCCESS: Logget ind")) { isLoggedIn = true; System.out.println("\n" + serverMessage); showMainMenu(); }
        else if (serverMessage.startsWith("FEJL: Login mislykkedes.")) { System.out.println(serverMessage); System.out.print("Enter your username: "); }
        else if (serverMessage.equals("START_CHATROOM_LIST")) { displayChatroomList(controlIn); }
        else if (serverMessage.startsWith("SUCCESS: Logget ud")) { isLoggedIn = false; System.out.println("\n" + serverMessage); closeConnection(); }
        else {
            // Using a carriage return `\r` clears the current line (e.g., "Select an action: ")
            // before printing the server message, which keeps the console tidy.
            System.out.print("\r" + serverMessage + "\n");
            if(isLoggedIn && clientState == ClientState.AWAITING_MENU_CHOICE) {
               System.out.print("Select an action: ");
            }
        }
    }

    // Reads and displays the list of chatrooms from the server.
    private void displayChatroomList(DataInputStream dataIn) throws IOException {
        System.out.println("\n--- Available Chatrooms ---");
        chatroomMap.clear();
        String line;
        while (!(line = dataIn.readUTF()).equals("END_CHATROOM_LIST")) {
            String[] parts = line.split("::", 2);
            if (parts.length == 2) {
                System.out.println("- " + parts[1]);
                chatroomMap.put(parts[1].toLowerCase(), UUID.fromString(parts[0]));
            }
        }
        System.out.print("Enter the name of the chatroom you wish to join: ");
        clientState = ClientState.AWAITING_CHATROOM_CHOICE;
    }

    public static void main(String[] args) {
        System.out.println("Preparing local directories...");
        new File("Downloads").mkdirs(); new File("uploads").mkdirs();
        System.out.println("Ready to start.");
        new ClientUI();
    }
}