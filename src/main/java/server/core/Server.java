package server.core;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

import Services.DataTransferManager;
import Services.LoginService;
import Services.ThreadExecutorService;
import Services.UserHandler;
import common.models.Datahandler;

import server.utils.Logger;
import server.utils.Logger.LogEvent;

// The main entry point for the chat server application.
// This class is responsible for initializing services and listening for client connections.
public class Server {
    private final int controlPort; // Port for commands, chat messages, and login.
    private final int dataPort;    // Port dedicated to high-throughput file transfers.
    private final ThreadExecutorService threadPool;
    private final LoginService loginService;
    private final Datahandler datahandler;

    // The constructor uses dependency injection, receiving services from the main method.
    // This makes the server more modular and easier to test.
    public Server(int controlPort, int dataPort, LoginService loginService, Datahandler datahandler) {
        this.controlPort = controlPort;
        this.dataPort = dataPort;
        this.threadPool = new ThreadExecutorService(Runtime.getRuntime().availableProcessors());
        this.loginService = loginService; 
        this.datahandler = datahandler;  
    }

    public void start() {
        // The data listener runs on a separate thread to avoid blocking the main control listener.
        new Thread(this::listenForDataConnections).start();
        
        // The main thread is used for listening to control connections.
        listenForControlConnections();
    }

    // Listens for incoming client connections on the control port.
    private void listenForControlConnections() {
        try (ServerSocket serverSocket = new ServerSocket(controlPort)) {
            Logger.info(LogEvent.SERVER_LIFECYCLE, "Control server is live and listening on port: " + controlPort);
            while (true) {
                Socket socket = serverSocket.accept();
                Logger.info(LogEvent.USER_SESSION,("New control connection from: " + socket.getInetAddress()));
                // Each new connection is handed off to a ServerHandler in the thread pool.
                threadPool.addJob(new ServerHandler(socket, loginService, datahandler));
            }
        } catch (IOException e) {
            Logger.error(LogEvent.SYSTEM_ERROR,"Server error: Could not start on control port " + controlPort, e);
            e.printStackTrace();
        }
    }

    // Listens for incoming client connections on the dedicated data port for file transfers.
    private void listenForDataConnections() {
        try (ServerSocket dataSocket = new ServerSocket(dataPort)) {
            Logger.info(LogEvent.SERVER_LIFECYCLE,"Data server is live and listening on port: " + dataPort);
            while (true) {
                Socket socket = dataSocket.accept();
                Logger.info(LogEvent.FILE_TRANSFER,"New data connection from: " + socket.getInetAddress());

                // A client connecting to the data port must immediately send its unique transfer ID
                // to be paired with its counterpart.
                try {
                    DataInputStream dataIn = new DataInputStream(socket.getInputStream());
                    UUID transferId = UUID.fromString(dataIn.readUTF());
            
                    DataTransferManager.handleNewDataConnection(transferId, socket);
                } catch (IOException e) {
                    Logger.error(LogEvent.SYSTEM_ERROR,"Error handling new data connection: " + e.getMessage(), e);
                    try { socket.close(); } catch (IOException ignored) {}
                }
            }
        } catch (IOException e) {
            Logger.error(LogEvent.SYSTEM_ERROR,"Server error: Could not start on data port " + dataPort, e);
            e.printStackTrace();
        }
    }

    // Application entry point.
    public static void main(String[] args) {
        // Initialize all required services here.
        UserHandler userHandler = new UserHandler();
        LoginService loginService = new LoginService(userHandler);
        Datahandler datahandler = new Datahandler();

        // Inject the services into the server instance.
        Server server = new Server(5010, 5011, loginService, datahandler);
        server.start();
    }
}