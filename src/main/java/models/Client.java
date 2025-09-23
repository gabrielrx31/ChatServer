package models;

public class Client {
    private String username;
    private String ipAddress;

    public Client(String username, String ipAddress) {
        this.username = username;
        this.ipAddress = ipAddress;
    }

    public String getUsername() {
        return username;
    }

    public String getIpAddress() {
        return ipAddress;
    }
    @Override
    public String toString() {
        return "Client{" +
                "username='" + username + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
