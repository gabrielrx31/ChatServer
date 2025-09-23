package models;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import Services.ThreadExecutorService; // Korrekt pakke og klassenavn

public class Server {
    private int port;
    private ThreadExecutorService threadPool;

    // Konstruktøren tager nu kun porten
    public Server (int port){
        this.port = port;
        // Opretter en enkelt instans af din ThreadExecutorService
        this.threadPool = new ThreadExecutorService(Runtime.getRuntime().availableProcessors());
    }

    public void start(){
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("serveren er live gennem port: " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Klient har nu forbindelse: " + socket.getInetAddress());

                // Opret en ny ServerHandler-opgave for hver klient
                threadPool.addJob(new ServerHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Sørg for at lukke puljen, når serveren stopper
            threadPool.shutdown();
        }
    }
    
    public static void main(String[] args) {
        Server server = new Server(5010);
        server.start();
    }
}