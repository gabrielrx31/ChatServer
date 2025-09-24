package Services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Handles the logistics of pairing two clients for a direct file transfer.
public class DataTransferManager {
    // A temporary holding area for clients who have connected to the data port
    // and are waiting for their counterpart. Maps a transfer ID to the first client's socket.
    private static final Map<UUID, Socket> pendingTransfers = new ConcurrentHashMap<>();
    // Stores the expected file size for each transfer, crucial for knowing when to stop reading.
    private static final Map<UUID, Long> transferSizes = new ConcurrentHashMap<>();

    // This method is called by the Server's data port listener thread.
    // It acts as a rendezvous point for the sender and receiver.
    public static void handleNewDataConnection(UUID transferId, Socket socket) {
        if (pendingTransfers.containsKey(transferId)) {
            // The second party has arrived. Let's start the transfer.
            Socket firstPartySocket = pendingTransfers.remove(transferId);
            long fileSize = transferSizes.remove(transferId);

            System.out.println("DataTransferManager: Pair found for transfer " + transferId + ". Starting relay.");
            // The relay runs in a new thread to avoid blocking the data port listener.
            // Assumption: firstPartySocket is the sender, and the new 'socket' is the receiver.
            new Thread(() -> relayFileStream(firstPartySocket, socket, fileSize)).start();
        } else {
            // This is the first party to connect. We'll store their socket and wait.
            System.out.println("DataTransferManager: First party arrived for transfer " + transferId + ". Waiting for counterpart.");
            pendingTransfers.put(transferId, socket);
        }
    }

    // Called by the ServerHandler to log the file size before clients connect to the data port.
    public static void registerTransferSize(UUID transferId, long fileSize) {
        transferSizes.put(transferId, fileSize);
    }
    
    // The core of the transfer logic. It pipes bytes from the sender to the receiver.
    // The server itself never stores the file on disk.
    private static void relayFileStream(Socket senderSocket, Socket receiverSocket, long fileSize) {
        try (InputStream fromSender = senderSocket.getInputStream();
             OutputStream toReceiver = receiverSocket.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRelayed = 0;

            System.out.println("Relay started: Forwarding " + fileSize + " bytes.");
            // The loop continues until the expected number of bytes has been relayed.
            // This is more reliable than waiting for the stream to end (-1).
            while (totalBytesRelayed < fileSize && (bytesRead = fromSender.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalBytesRelayed))) != -1) {
                toReceiver.write(buffer, 0, bytesRead);
                totalBytesRelayed += bytesRead;
            }
            toReceiver.flush();
            System.out.println("Relay complete: " + totalBytesRelayed + " bytes forwarded.");

        } catch (IOException e) {
            System.err.println("Error during file relay: " + e.getMessage());
        } finally {
            // Clean up by closing both sockets involved in the transfer.
            try { senderSocket.close(); } catch (IOException e) { /* Ignored */ }
            try { receiverSocket.close(); } catch (IOException e) { /* Ignored */ }
        }
    }
}