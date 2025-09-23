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
    private enum ClientState {
        AWAITING_MENU_CHOICE, AWAITING_CHATROOM_CHOICE, AWAITING_MESSAGE,
        AWAITING_SYSTEM_MESSAGE, AWAITING_FILE_ACCEPT
    }

    private static final String SERVER_ADDRESS = "localhost";
    private final int controlPort = 5010;

    private Socket controlSocket;
    private DataOutputStream controlOut;
    private DataInputStream controlIn;
    
    private Scanner scanner;
    private UUID chatroomId;
    private boolean isLoggedIn = false;
    private String username;
    private Map<String, UUID> chatroomMap = new HashMap<>();
    private volatile ClientState clientState = ClientState.AWAITING_MENU_CHOICE;

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
            
            System.out.println("Forbindelse etableret til serveren på port " + controlPort);
            
            new Thread(new ServerListener()).start();
            runClientUI();
        } catch (IOException e) {
            System.err.println("Fejl: Kunne ikke oprette forbindelse til serveren. " + e.getMessage());
        }
    }
    
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
            System.err.println("Fejl under afsendelse af kommando: " + e.getMessage());
        }
    }

    private void showLoginMenu() {
        System.out.println("--- Login-menu ---");
        System.out.print("Indtast dit brugernavn: ");
    }
    
    private void showMainMenu() {
        System.out.println("\n--- Hovedmenu ---\n1. Send besked til chatrum\n2. Skift chatrum\n3. Send system besked\n4. Log ud\n5. Send en fil");
        System.out.print("Vælg en handling: ");
        clientState = ClientState.AWAITING_MENU_CHOICE;
    }

    private void handleMenuChoice(String choice) throws IOException {
        switch (choice) {
            case "1": sendMessageToChatroom(); break;
            case "2": changeChatroom(); break;
            case "3": sendSystemMessage(); break;
            case "4": logout(); break;
            case "5": initiateFileTransfer(); break;
            default: System.out.println("Ugyldigt valg. Prøv igen."); showMainMenu(); break;
        }
    }

    private void handleChatroomChoice(String roomName) throws IOException {
        String lowerCaseRoomName = roomName.toLowerCase();
        if (chatroomMap.containsKey(lowerCaseRoomName)) {
            this.chatroomId = chatroomMap.get(lowerCaseRoomName);
            controlOut.writeUTF("JOIN_ROOM::" + this.chatroomId.toString());
            System.out.println("Deltager i chatrum: " + roomName);
        } else {
            System.out.println("Ugyldigt chatrumsnavn. Prøv igen.");
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
        if (chatroomId == null) { System.out.println("Vælg et chatrum, før du sender en besked."); changeChatroom(); }
        else { System.out.print("Skriv din besked: "); clientState = ClientState.AWAITING_MESSAGE; }
    }

    private void sendSystemMessage() throws IOException {
        if (chatroomId == null) { System.out.println("Vælg et chatrum, før du sender en besked."); changeChatroom(); }
        else { System.out.print("Skriv din systembesked: "); clientState = ClientState.AWAITING_SYSTEM_MESSAGE; }
    }

    private void changeChatroom() throws IOException { System.out.println("Henter tilgængelige chatrum..."); controlOut.writeUTF("LIST_ROOMS"); }
    
    private void initiateFileTransfer() throws IOException {
        System.out.print("Indtast brugernavnet på modtageren: ");
        this.fileToSendRecipient = scanner.nextLine();
        System.out.print("Indtast navnet på filen i din 'uploads' mappe: ");
        String filename = scanner.nextLine();
        this.fileToSendPath = "uploads/" + filename;
        File file = new File(fileToSendPath);
        if (file.exists() && !file.isDirectory()) {
            long fileSize = file.length();
            controlOut.writeUTF("WANT_TO_SEND_FILE::" + fileToSendRecipient + "::" + filename + "::" + fileSize);
        } else {
            System.out.println("Fejl: Filen '" + filename + "' findes ikke i din 'uploads' mappe.");
            showMainMenu();
        }
    }

    private void handleFileAccept(String answer) throws IOException {
        if (answer.equalsIgnoreCase("ja")) {
            controlOut.writeUTF("ACCEPT_FILE::" + this.fileTransferId);
            System.out.println("Accepterede filoverførsel. Venter på instruktioner fra server...");
        } else {
            controlOut.writeUTF("REJECT_FILE::" + this.fileTransferId);
            System.out.println("Afviste filoverførsel.");
        }
        this.fileTransferId = null;
        showMainMenu();
    }

    private void sendFile(int dataPort, UUID transferId) {
        try (Socket dataSocket = new Socket(SERVER_ADDRESS, dataPort);
             DataOutputStream dataOutStream = new DataOutputStream(dataSocket.getOutputStream());
             FileInputStream fis = new FileInputStream(this.fileToSendPath)) {
            
            System.out.println("Forbinder til data-port " + dataPort + " for at sende fil...");
            dataOutStream.writeUTF(transferId.toString());

            File file = new File(this.fileToSendPath);
            byte[] buffer = new byte[8192];
            int bytesRead;
            System.out.println("Starter afsendelse af fil: " + file.getName());
            while ((bytesRead = fis.read(buffer)) != -1) {
                dataOutStream.write(buffer, 0, bytesRead);
            }
            dataOutStream.flush();
            System.out.println("Fil afsendt succesfuldt. Lukker data-forbindelse.");

        } catch (IOException e) {
            System.err.println("Fejl under afsendelse af fil: " + e.getMessage());
        } finally {
            this.fileToSendPath = null;
            this.fileToSendRecipient = null;
            showMainMenu();
        }
    }

    private void receiveFile(int dataPort, UUID transferId) {
        try (Socket dataSocket = new Socket(SERVER_ADDRESS, dataPort);
             DataOutputStream dataOutStream = new DataOutputStream(dataSocket.getOutputStream());
             DataInputStream dataInStream = new DataInputStream(dataSocket.getInputStream());
             FileOutputStream fos = new FileOutputStream("Downloads/" + this.incomingFilename)) {

            System.out.println("Forbinder til data-port " + dataPort + " for at modtage fil...");
            dataOutStream.writeUTF(transferId.toString());

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesReceived = 0;
            System.out.println("Modtager fil: " + this.incomingFilename);
            while (totalBytesReceived < this.incomingFileSize && (bytesRead = dataInStream.read(buffer, 0, (int)Math.min(buffer.length, this.incomingFileSize - totalBytesReceived))) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytesReceived += bytesRead;
            }
            System.out.println("Fil modtaget succesfuldt og gemt i Downloads mappen. Lukker data-forbindelse.");

        } catch (IOException e) {
            System.err.println("Fejl under modtagelse af fil: " + e.getMessage());
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
                        System.out.println("\n[Filoverførsel] " + fromUser + " vil sende dig: " + incomingFilename + " (" + incomingFileSize + " bytes).");
                        System.out.print("Accepter? (ja/nej): ");
                        clientState = ClientState.AWAITING_FILE_ACCEPT;
                    } 
                    else if (serverMessage.startsWith("START_FILE_TRANSFER::")) {
                        String[] parts = serverMessage.split("::", 3);
                        int dataPort = Integer.parseInt(parts[1]);
                        UUID transferId = UUID.fromString(parts[2]);
                        System.out.println("\nINFO: Modtager accepterede. Forbereder overførsel...");
                        new Thread(() -> sendFile(dataPort, transferId)).start();
                    } 
                    else if (serverMessage.startsWith("PROCEED_WITH_DOWNLOAD::")) {
                        String[] parts = serverMessage.split("::", 3);
                        int dataPort = Integer.parseInt(parts[1]);
                        UUID transferId = UUID.fromString(parts[2]);
                        System.out.println("\nINFO: Serveren er klar. Starter download...");
                        new Thread(() -> receiveFile(dataPort, transferId)).start();
                    }
                    else if (serverMessage.startsWith("REJECT_FILE_TRANSFER::")) {
                        System.out.println("\nINFO: " + serverMessage.split("::", 2)[1] + " afviste filoverførslen.");
                        showMainMenu();
                    }
                    else {
                        // Håndter alle andre normale tekst-baserede beskeder
                        handleStandardMessages(serverMessage);
                    }
                }
            } catch (IOException e) {
                System.out.println("\nForbindelse til serveren mistet.");
            } finally {
                closeConnection();
            }
        }
    }
    
    private void handleStandardMessages(String serverMessage) throws IOException {
        if (serverMessage.startsWith("SUCCESS: Logget ind")) { isLoggedIn = true; System.out.println("\n" + serverMessage); showMainMenu(); }
        else if (serverMessage.startsWith("FEJL: Login mislykkedes.")) { System.out.println(serverMessage); System.out.print("Indtast dit brugernavn: "); }
        else if (serverMessage.equals("START_CHATROOM_LIST")) { displayChatroomList(controlIn); }
        else if (serverMessage.startsWith("SUCCESS: Logget ud")) { isLoggedIn = false; System.out.println("\n" + serverMessage); closeConnection(); }
        else {
            System.out.print("\r" + serverMessage + "\n");
            if(isLoggedIn && clientState == ClientState.AWAITING_MENU_CHOICE) {
               System.out.print("Vælg en handling: ");
            }
        }
    }

    private void displayChatroomList(DataInputStream dataIn) throws IOException {
        System.out.println("\n--- Tilgængelige chatrum ---");
        chatroomMap.clear();
        String line;
        while (!(line = dataIn.readUTF()).equals("END_CHATROOM_LIST")) {
            String[] parts = line.split("::", 2);
            if (parts.length == 2) {
                System.out.println("- " + parts[1]);
                chatroomMap.put(parts[1].toLowerCase(), UUID.fromString(parts[0]));
            }
        }
        System.out.print("Indtast navnet på det chatrum, du vil deltage i: ");
        clientState = ClientState.AWAITING_CHATROOM_CHOICE;
    }

    public static void main(String[] args) {
        System.out.println("Forbereder mapper...");
        new File("Downloads").mkdirs(); new File("uploads").mkdirs();
        System.out.println("Klar til start.");
        new ClientUI();
    }
}