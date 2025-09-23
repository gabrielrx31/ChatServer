package models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private Client client; 

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            // Første besked fra klienten er et "username"
            out.println("Enter username:");
            String username = in.readLine();

            
            client = new Client(username, clientSocket.getInetAddress().toString());
            System.out.println("New client model created: " + client);

            // Kommunikation loop
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(client.getUsername() + ": " + message);
                out.println("Echo from server: " + message);
            }

        } catch (IOException e) {
            System.err.println("Error with client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();                                   // ? er en kort form for if-else
                System.out.println("Client disconnected: " + (client != null ? client.getUsername() : "Unknown"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

