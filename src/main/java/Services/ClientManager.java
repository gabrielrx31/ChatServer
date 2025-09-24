package Services;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// This class acts as a central registry for all connected clients.
// It allows any part of the server to send messages without needing direct access to socket objects.
public class ClientManager {
    // A thread-safe map that stores client sessions, mapping a unique session ID to a client's socket.
    private static final Map<UUID, Socket> clients = new ConcurrentHashMap<>();

    // Registers a new client when they connect.
    public static void addClient(UUID clientId, Socket socket) {
        clients.put(clientId, socket);
        System.out.println("New client added: " + clientId);
    }

    // Removes a client when they disconnect.
    public static void removeClient(UUID clientId) {
        clients.remove(clientId);
        System.out.println("Client removed: " + clientId);
    }

    // Retrieves a client's socket using their session ID.
    public static Socket getClientSocket(UUID clientId) {
        return clients.get(clientId);
    }

    // Sends a message to every connected client.
    public static void broadcastMessage(String message) {
        for (Socket socket : clients.values()) {
            try {
                // A new DataOutputStream is created for each message to ensure thread safety.
                new DataOutputStream(socket.getOutputStream()).writeUTF(message);
            } catch (IOException e) {
                System.err.println("Error during broadcast to client: " + e.getMessage());
                // In a real application, you might want to remove the client if the socket is broken.
            }
        }
    }

    // Sends a message to a single, specific client.
    public static void unibroadcastMessage(UUID clientId, String message) {
        Socket socket = clients.get(clientId);
        if (socket != null && !socket.isClosed()) {
            try {
                new DataOutputStream(socket.getOutputStream()).writeUTF(message);
            } catch (IOException e) {
                System.err.println("Error during unibroadcast to client " + clientId + ": " + e.getMessage());
            }
        } else {
            System.err.println("Recipient client not found or connection is closed: " + clientId);
        }
    }
}