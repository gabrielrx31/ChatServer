package server.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

// Note: This class seems to be an older implementation. The core file transfer logic
// is now handled by the relayFileStream method in DataTransferManager.
public class DataRelay implements Runnable {
    private final Socket sender;
    private final Socket receiver;

    public DataRelay(Socket sender, Socket receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    @Override
    public void run() {
        // This method acts as a simple pipe, reading bytes from the sender
        // and immediately writing them to the receiver.
        try (InputStream fromSender = sender.getInputStream();
             OutputStream toReceiver = receiver.getOutputStream()) {
            
            byte[] buffer = new byte[8192]; // A reasonably sized buffer for I/O.
            int bytesRead;
            while ((bytesRead = fromSender.read(buffer)) != -1) {
                toReceiver.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            System.err.println("DataRelay Error: " + e.getMessage());
        } finally {
            // Ensure both sockets are closed to release resources, even if an error occurred.
            try { sender.close(); } catch (Exception e) { /* ignored */ }
            try { receiver.close(); } catch (Exception e) { /* ignored */ }
            System.out.println("DataRelay: Transfer complete, both data sockets closed.");
        }
    }
}