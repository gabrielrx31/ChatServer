package models;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private int port;
    private ExecutorService threadPool;

    public Server (int port, int maxClients){
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(maxClients);
    }
    public void start(){

            try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("serveren er live gennem port: " + port);

                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("Client har nu forbindelse: " + socket.getInetAddress());

                    
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                threadPool.shutdown();
            }
        }
    
    public static void main(String[] args) {
        Server server = new Server(5001, 20);
        server.start();
    }
}
