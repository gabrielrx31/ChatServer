package models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

import models.protocols.FileTransferProtocol;
import models.protocols.MessageProtocol;

public class ServerHandler implements Runnable {
    private final Socket clientSocket;
    private final UUID clientId;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isAuthenticated = false; // Status for login

    public ServerHandler(Socket socket) {
        this.clientSocket = socket;
        this.clientId = UUID.randomUUID(); // Unikt ID til hver handler
    }

    @Override
    public void run() {
        System.out.println("Starter ServerHandler for klient: " + clientId);
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("Modtaget fra klient " + clientId + ": " + clientMessage);
                handleClientMessage(clientMessage);
            }

        } catch (IOException e) {
            System.err.println("Fejl under håndtering af klient " + clientId + ": " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void handleClientMessage(String message) {
        String[] parts = message.split("::");
        String command = parts[0];

        // Her håndteres de forskellige protokolkommandoer
        switch (command) {
            case "LOGIN":
                // Login-logik her
                break;
            case MessageProtocol.COMMAND_SEND_MSG:
                handleTextMessage(message);
                break;
            case FileTransferProtocol.COMMAND_SEND_FILE:
                // Filhåndtering her
                break;
            case "LOGOUT":
                // Logout-logik her
                break;
            default:
                out.println("Ugyldig kommando: " + command);
                break;
        }
    }

    private void handleTextMessage(String message) {
        MessageProtocol msgProtocol = new MessageProtocol();
        MessageModel parsedMessage = msgProtocol.parseMessage(message);

        if (parsedMessage != null) {
            System.out.println("Besked fra: " + parsedMessage.sender + " i chatrum: " + parsedMessage.chatroomId + " - " + parsedMessage.content);
            // Videresend beskeden til andre klienter i chatrummet
            // Dette ville normalt involvere en manager-klasse, der holder styr på alle clients
            // f.eks. chatroomManager.broadcastMessage(parsedMessage);
        } else {
            out.println("Fejl: Ugyldigt beskedformat.");
        }
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
            System.out.println("Forbindelse lukket for klient: " + clientId);
        } catch (IOException e) {
            System.err.println("Fejl ved lukning af forbindelse: " + e.getMessage());
        }
    }
}