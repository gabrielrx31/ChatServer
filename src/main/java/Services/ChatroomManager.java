package Services;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import common.models.ChatRoom;

public class ChatroomManager {
    private static final Map<UUID, ChatRoom> chatrooms = new ConcurrentHashMap<>();

    // Opretter et par standard chatrum, når serveren starter
    static {
        // Bruger en tilfældig UUID som "system-bruger" for at oprette rummene
        UUID systemId = UUID.randomUUID();
        
        ChatRoom lobby = new ChatRoom("Lobby", systemId);
        chatrooms.put(lobby.getId(), lobby);
        System.out.println("Chatroom oprettet: Lobby med ID " + lobby.getId());

        ChatRoom general = new ChatRoom("General", systemId);
        chatrooms.put(general.getId(), general);
        System.out.println("Chatroom oprettet: General med ID " + general.getId());
    }

    public static ChatRoom getChatroomById(UUID id) {
        return chatrooms.get(id);
    }

    public static ChatRoom createChatroom(String roomName, UUID createdBy) {
        ChatRoom newRoom = new ChatRoom(roomName, createdBy);
        chatrooms.put(newRoom.getId(), newRoom);
        return newRoom;
    }

    public static Map<UUID, ChatRoom> getAllChatrooms() {
        return chatrooms;
    }
}