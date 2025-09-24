package server.core;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

import Services.DataTransferManager;
import Services.LoginService;
import Services.ThreadExecutorService;
import common.models.Datahandler;



public class Server {
    private final int controlPort; // f.eks. 5010
    private final int dataPort;    // f.eks. 5011
    private final ThreadExecutorService threadPool;
    private final LoginService loginService;
    private final Datahandler datahandler;

    public Server(int controlPort, int dataPort) {
        this.controlPort = controlPort;
        this.dataPort = dataPort;
        this.threadPool = new ThreadExecutorService(Runtime.getRuntime().availableProcessors());
        this.loginService = new LoginService();
        this.datahandler = new Datahandler();
    }

    public void start() {
        // Start a new thread to listen for data connections
        new Thread(this::listenForDataConnections).start();
        
        // Use the main thread to listen for control connections (as before)
        listenForControlConnections();
    }

    private void listenForControlConnections() {
        try (ServerSocket serverSocket = new ServerSocket(controlPort)) {
            System.out.println("Control server is live and listening on port: " + controlPort);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New control connection from: " + socket.getInetAddress());
                threadPool.addJob(new ServerHandler(socket, loginService, datahandler));
            }
        } catch (IOException e) {
            System.err.println("Server error: Could not start on control port " + controlPort);
            e.printStackTrace();
        }
    }

    private void listenForDataConnections() {
        try (ServerSocket dataSocket = new ServerSocket(dataPort)) {
            System.out.println("Data server is live and listening on port: " + dataPort);
            while (true) {
                Socket socket = dataSocket.accept();
                System.out.println("New data connection from: " + socket.getInetAddress());
                // When a client connects, the first thing it must do is send a unique transfer ID.
                // We read the ID here and pass it to the manager.
                try {
                    DataInputStream dataIn = new DataInputStream(socket.getInputStream());
                    UUID transferId = UUID.fromString(dataIn.readUTF());
                    // This is the corrected line:
                    DataTransferManager.handleNewDataConnection(transferId, socket);
                } catch (IOException e) {
                    System.err.println("Error handling new data connection: " + e.getMessage());
                    try { socket.close(); } catch (IOException ignored) {}
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: Could not start on data port " + dataPort);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Server server = new Server(5010, 5011); // Define both ports
        server.start();
    }
}