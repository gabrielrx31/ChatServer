package Services;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientManager {
    private static final Map<UUID, Socket> clients = new ConcurrentHashMap<>();

    public static void addClient(UUID clientId, Socket socket) {
        clients.put(clientId, socket);
        System.out.println("Ny klient tilføjet: " + clientId);
    }

    public static void removeClient(UUID clientId) {
        clients.remove(clientId);
        System.out.println("Klient fjernet: " + clientId);
    }

    public static Socket getClientSocket(UUID clientId) {
        return clients.get(clientId);
    }

    public static void broadcastMessage(String message) {
        for (Socket socket : clients.values()) {
            try {
                // Brug DataOutputStream til at sende tekst med writeUTF
                new DataOutputStream(socket.getOutputStream()).writeUTF(message);
            } catch (IOException e) {
                System.err.println("Fejl ved broadcast til klient: " + e.getMessage());
            }
        }
    }

    public static void unibroadcastMessage(UUID clientId, String message) {
        Socket socket = clients.get(clientId);
        if (socket != null && !socket.isClosed()) {
            try {
                new DataOutputStream(socket.getOutputStream()).writeUTF(message);
            } catch (IOException e) {
                System.err.println("Fejl ved unibroadcast til klient " + clientId + ": " + e.getMessage());
            }
        } else {
            System.err.println("Modtagerklient ikke fundet eller forbindelsen er lukket: " + clientId);
        }
    }
}