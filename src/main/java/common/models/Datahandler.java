package common.models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

// This class acts as an in-memory database for chat messages.
public class Datahandler {
    
    private List<MessageModel> messagesArray;

    public Datahandler() {
        this.messagesArray = new ArrayList<>();
    }

    public void addMessageToArray(MessageModel message) {
        messagesArray.add(message);
    }

    // Safely removes a message using an iterator to avoid concurrency issues.
    public void deleteMessage(UUID id) {
        Iterator<MessageModel> iterator = messagesArray.iterator();
        while (iterator.hasNext()) {
            MessageModel message = iterator.next();
            if (message.messageId.equals(id)) {
                iterator.remove();
                break; 
            }
        }
    }

    public MessageModel getMessageById(UUID id) {
        for (MessageModel message : messagesArray) {
            if (message.messageId.equals(id)) {
                return message;
            }
        }
        return null;
    }

    // Finds a message by its ID and updates its content and timestamp.
    public void updateMessage(UUID id, String newText) {
        for (MessageModel message : messagesArray) {
            if (message.messageId.equals(id)) {
                message.content = newText;
                message.timestamp = java.time.LocalDateTime.now();
                break;
            }
        }
    }

    public List<MessageModel> getMessages() {
        return messagesArray;
    }
    
    // Allows replacing the entire message list, mainly for testing or initial setup.
    public void setMessages(List<MessageModel> messages) {
        this.messagesArray = messages;
    }
}