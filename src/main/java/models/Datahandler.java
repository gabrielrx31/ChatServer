package models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Datahandler {
    
    private List<MessageModel> messagesArray;

    public Datahandler() {
        this.messagesArray = new ArrayList<>();
    }

    public void addMessageToArray(MessageModel message) {
        messagesArray.add(message);
    }
    public void deleteMessage(UUID id) {
        int count = 0;

        for (MessageModel message : messagesArray ) {
            count++;
            if (message.messageId.equals(id)) {
                messagesArray.remove(count);
            }

        } 
    }


    public MessageModel getMessageById (UUID id){
      MessageModel foundMessage = null; 

        for (MessageModel message : messagesArray ) {
            
            
            if (!message.messageId.equals(id)) {
                System.out.println("besked kan ikke findes");
                
            } else {
                foundMessage = message;
            }
        
        } return foundMessage;
        
    }

    public void updateMessage (UUID id, String newText){
        for(MessageModel message : messagesArray) {
            if(message.messageId.equals(id)){
                message.content = newText;
                message.timestamp = java.time.LocalDateTime.now();
                break;
            }
        }
    }

    public List<MessageModel> getMessages() {
        return messagesArray;
    }
    public void setMessages(List<MessageModel> messages) {
        this.messagesArray = messages;
    }
}
