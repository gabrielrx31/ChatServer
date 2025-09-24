package common.models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Represents a single chat room where users can interact.
public class ChatRoom {
    private UUID id;
    private String roomName;
    // Stores the unique IDs of users currently in the room.
    private Set<UUID> members;
    private LocalDateTime createdAt;
    private UUID createdBy;
    private int maxMembers;

    public ChatRoom(String roomName, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.roomName = roomName;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.members = new HashSet<>();
        this.maxMembers = 50;
        // The user who creates the room is automatically added as a member.
        this.members.add(createdBy);
    }
    
    // Getters
    public UUID getId() { return id; }
    public String getRoomName() { return roomName; }
    // Returns a copy of the member set to prevent direct modification from outside.
    public Set<UUID> getMembers() { return new HashSet<>(members); }
    public int getMaxMembers() { return maxMembers; }
    
    // Adds a user to the chat room, but only if it's not full.
    public boolean addMember(UUID userId) {
        if (members.size() < maxMembers) {
            return members.add(userId);
        }
        return false; // Room is full.
    }

    // Removes a user from the chat room.
    public boolean removeMember(UUID userId) {
        return members.remove(userId);
    }
}