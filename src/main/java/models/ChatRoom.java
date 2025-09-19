package models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChatRoom {
    private UUID id;
    private String roomName;
    private Set<UUID> members; // Bruger UUID'er til at reference brugere
    private LocalDateTime createdAt;
    private UUID createdBy;
    private int maxMembers;

    // Constructor
    public ChatRoom(String roomName, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.roomName = roomName;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.members = new HashSet<>();
        this.maxMembers = 50;
        
        //Add creator as the first member
        this.members.add(createdBy);
    }

    // Constructor with maxMembers parameter
    public ChatRoom(String roomName, UUID createdBy, int maxMembers) {
        this(roomName, createdBy);
        this.maxMembers = maxMembers;
    }

    // Method to add a member
    public boolean addMember(UUID userId) {
        if (members.size() < maxMembers) {
            return members.add(userId);
        }
        return false; 
    }

    // Method to remove a member
    public boolean removeMember(UUID userId) {
        return members.remove(userId);
    }

    // Method to check if a user is a member
    public boolean isMember(UUID userId) {
        return members.contains(userId);
    }

    // Method to check if the room is empty
    public boolean isEmpty() {
        return members.isEmpty();
    }

    // Method to get the current member count
    public int getMemberCount() {
        return members.size();
    }

    // Method to check if the room is full
    public boolean isFull() {
        return members.size() >= maxMembers;
    }

    // Getters og setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Set<UUID> getMembers() {
        return new HashSet<>(members); 
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    @Override
    public String toString() {
        return "ChatRoom{" +
                "id=" + id +
                ", roomName='" + roomName + '\'' +
                ", memberCount=" + members.size() +
                ", maxMembers=" + maxMembers +
                ", createdAt=" + createdAt +
                '}';
    }
}
