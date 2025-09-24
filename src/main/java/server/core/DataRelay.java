package server.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class DataRelay implements Runnable {
    private final Socket sender;
    private final Socket receiver;

    public DataRelay(Socket sender, Socket receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    @Override
    public void run() {
        try (InputStream fromSender = sender.getInputStream();
             OutputStream toReceiver = receiver.getOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fromSender.read(buffer)) != -1) {
                toReceiver.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            System.err.println("DataRelay fejl: " + e.getMessage());
        } finally {
            try { sender.close(); } catch (Exception e) { /* ignored */ }
            try { receiver.close(); } catch (Exception e) { /* ignored */ }
            System.out.println("DataRelay: Overførsel afsluttet, begge data-sockets lukket.");
        }
    }
}