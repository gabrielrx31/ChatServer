package Services;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import server.utils.Logger;
import server.utils.Logger.LogEvent;

// This class acts as a central registry for all connected clients.
// It allows any part of the server to send messages without needing direct access to socket objects.
public class ClientManager {
    // A thread-safe map that stores client sessions, mapping a unique session ID to a client's socket.
    private static final Map<UUID, Socket> clients = new ConcurrentHashMap<>();

    // Registers a new client when they connect.
    public static void addClient(UUID clientId, Socket socket) {
        clients.put(clientId, socket);
        Logger.info(LogEvent.USER_SESSION, "New client added: " + clientId + " from " + socket.getRemoteSocketAddress());
    }

    // Removes a client when they disconnect.
    public static void removeClient(UUID clientId) {
        Socket removed = clients.remove(clientId);
        if (removed != null) {
            Logger.info(LogEvent.USER_SESSION, "Client removed: " + clientId);
        } else {
            Logger.warning(LogEvent.USER_SESSION, "Attempted to remove non-existent client: " + clientId);
        }
    }

    // Retrieves a client's socket using their session ID.
    public static Socket getClientSocket(UUID clientId) {
        return clients.get(clientId);
    }

    // Sends a message to every connected client.
    public static void broadcastMessage(String message) {
        for (Map.Entry<UUID, Socket> entry : clients.entrySet()) {
            UUID clientId = entry.getKey();
            Socket socket = entry.getValue();
            try {
                new DataOutputStream(socket.getOutputStream()).writeUTF(message);
                Logger.info(LogEvent.CHAT_MESSAGE, "Broadcast message sent to client " + clientId);
            } catch (IOException e) {
                Logger.error(LogEvent.CHAT_MESSAGE, "Broadcast failed for client " + clientId, e);
            }
        }
    }

    // Sends a message to a single, specific client.
    public static void unibroadcastMessage(UUID clientId, String message) {
        Socket socket = clients.get(clientId);
        if (socket != null && !socket.isClosed()) {
            try {
                new DataOutputStream(socket.getOutputStream()).writeUTF(message);
                Logger.info(LogEvent.CHAT_MESSAGE, "Unicast message sent to client " + clientId);
            } catch (IOException e) {
                Logger.error(LogEvent.CHAT_MESSAGE, "Unicast failed for client " + clientId, e);
            }
        } else {
            Logger.warning(LogEvent.CHAT_MESSAGE, "Recipient client not found or connection closed: " + clientId);
        }
    }
}