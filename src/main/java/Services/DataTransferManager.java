package Services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import server.utils.Logger;
import server.utils.Logger.LogEvent;

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

            Logger.info(LogEvent.FILE_TRANSFER, "Pair found for transfer " + transferId + ". Starting relay.");
            // The relay runs in a new thread to avoid blocking the data port listener.
            new Thread(() -> relayFileStream(firstPartySocket, socket, fileSize, transferId)).start();
        } else {
            // This is the first party to connect. We'll store their socket and wait.
            pendingTransfers.put(transferId, socket);
            Logger.info(LogEvent.FILE_TRANSFER, "First party arrived for transfer " + transferId + ". Waiting for counterpart.");
        }
    }

    // Called by the ServerHandler to log the file size before clients connect to the data port.
    public static void registerTransferSize(UUID transferId, long fileSize) {
        transferSizes.put(transferId, fileSize);
        Logger.info(LogEvent.FILE_TRANSFER, "Registered transfer size for " + transferId + ": " + fileSize + " bytes");
    }
    
    // The core of the transfer logic. It pipes bytes from the sender to the receiver.
    // The server itself never stores the file on disk.
    private static void relayFileStream(Socket senderSocket, Socket receiverSocket, long fileSize, UUID transferId) {
        try (InputStream fromSender = senderSocket.getInputStream();
             OutputStream toReceiver = receiverSocket.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRelayed = 0;

            Logger.info(LogEvent.FILE_TRANSFER, "Relay started for transfer " + transferId + ". Forwarding " + fileSize + " bytes.");
            while (totalBytesRelayed < fileSize &&
                   (bytesRead = fromSender.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalBytesRelayed))) != -1) {
                toReceiver.write(buffer, 0, bytesRead);
                totalBytesRelayed += bytesRead;
            }
            toReceiver.flush();
            Logger.info(LogEvent.FILE_TRANSFER, "Relay complete for transfer " + transferId + ": " + totalBytesRelayed + " bytes forwarded.");

        } catch (IOException e) {
            Logger.error(LogEvent.FILE_TRANSFER, "Error during file relay for transfer " + transferId, e);
        } finally {
            try { senderSocket.close(); }
            catch (IOException e) { Logger.warning(LogEvent.FILE_TRANSFER, "Failed to close sender socket for transfer " + transferId + ": " + e.getMessage()); }
            try { receiverSocket.close(); }
            catch (IOException e) { Logger.warning(LogEvent.FILE_TRANSFER, "Failed to close receiver socket for transfer " + transferId + ": " + e.getMessage()); }
        }
    }
}
