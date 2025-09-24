package Services;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import common.models.ChatRoom;

// Manages all chat rooms on the server, including creation and retrieval.
public class ChatroomManager {
    // A thread-safe map to store all active chat rooms.
    private static final Map<UUID, ChatRoom> chatrooms = new ConcurrentHashMap<>();

    // This static block runs once when the server starts.
    // It initializes the server with a set of default chat rooms.
    static {
        // A temporary system user is created to be the owner of the default rooms.
        UUID systemId = UUID.randomUUID();
        
        ChatRoom lobby = new ChatRoom("Lobby", systemId);
        chatrooms.put(lobby.getId(), lobby);
        System.out.println("Chatroom created: Lobby with ID " + lobby.getId());

        ChatRoom general = new ChatRoom("General", systemId);
        chatrooms.put(general.getId(), general);
        System.out.println("Chatroom created: General with ID " + general.getId());
    }

    // Retrieves a single chat room by its unique ID.
    public static ChatRoom getChatroomById(UUID id) {
        return chatrooms.get(id);
    }

    // Creates a new chat room and adds it to the central map.
    public static ChatRoom createChatroom(String roomName, UUID createdBy) {
        ChatRoom newRoom = new ChatRoom(roomName, createdBy);
        chatrooms.put(newRoom.getId(), newRoom);
        return newRoom;
    }

    // Returns a map of all currently available chat rooms.
    public static Map<UUID, ChatRoom> getAllChatrooms() {
        return chatrooms;
    }
}