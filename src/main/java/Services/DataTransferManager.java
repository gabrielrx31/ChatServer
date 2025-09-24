package Services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DataTransferManager {
    // Denne klasse holder styr på afventende filoverførsler.
    private static final Map<UUID, Socket> pendingTransfers = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> transferSizes = new ConcurrentHashMap<>();

    /**
     * Kaldes når en klient forbinder til data-porten.
     * Hvis den er den første, bliver dens socket gemt.
     * Hvis den er den anden, bliver den parret med den første, og overførslen starter.
     */
    public static void handleNewDataConnection(UUID transferId, Socket socket) {
        if (pendingTransfers.containsKey(transferId)) {
            // Den anden part har forbundet. Hent den første parts socket.
            Socket firstPartySocket = pendingTransfers.remove(transferId);
            long fileSize = transferSizes.remove(transferId);

            // Start videresendelsen i en ny tråd for ikke at blokere.
            System.out.println("DataTransferManager: Par fundet for overførsel " + transferId + ". Starter relay.");
            // Vi antager her, at 'firstPartySocket' er afsenderen, og 'socket' er modtageren.
            new Thread(() -> relayFileStream(firstPartySocket, socket, fileSize)).start();
        } else {
            // Dette er den første part. Gem dens socket og vent på den anden.
            System.out.println("DataTransferManager: Første part ankommet for overførsel " + transferId + ". Venter på modpart.");
            pendingTransfers.put(transferId, socket);
        }
    }

    /**
     * ServerHandler kalder denne metode for at registrere en filstørrelse.
     * DENNE METODE MANGLER I DIN NUVÆRENDE VERSION.
     */
    public static void registerTransferSize(UUID transferId, long fileSize) {
        transferSizes.put(transferId, fileSize);
    }
    
    /**
     * Denne metode tager to sockets og sender alt data fra den ene til den anden.
     */
    private static void relayFileStream(Socket senderSocket, Socket receiverSocket, long fileSize) {
        try (InputStream fromSender = senderSocket.getInputStream();
             OutputStream toReceiver = receiverSocket.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRelayed = 0;

            System.out.println("Relay starter: Videresender " + fileSize + " bytes.");
            while (totalBytesRelayed < fileSize && (bytesRead = fromSender.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalBytesRelayed))) != -1) {
                toReceiver.write(buffer, 0, bytesRead);
                totalBytesRelayed += bytesRead;
            }
            toReceiver.flush();
            System.out.println("Relay fuldført: " + totalBytesRelayed + " bytes videresendt.");

        } catch (IOException e) {
            System.err.println("Fejl under fil-relay: " + e.getMessage());
        } finally {
            try { senderSocket.close(); } catch (IOException e) { /* Ignored */ }
            try { receiverSocket.close(); } catch (IOException e) { /* Ignored */ }
        }
    }
}