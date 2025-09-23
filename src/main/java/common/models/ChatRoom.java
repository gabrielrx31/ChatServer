package common.models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChatRoom {
    private UUID id;
    private String roomName;
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
        this.members.add(createdBy);
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public String getRoomName() { return roomName; }
    public Set<UUID> getMembers() { return new HashSet<>(members); }
    public int getMaxMembers() { return maxMembers; }
    
    // Public methods
    public boolean addMember(UUID userId) {
        if (members.size() < maxMembers) {
            return members.add(userId);
        }
        return false;
    }

    public boolean removeMember(UUID userId) {
        return members.remove(userId);
    }
}