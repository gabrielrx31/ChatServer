package models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Datahandler {
    
    private List<Message> messagesArray;
// UUID i default constructor?

    public Datahandler() {
        this.messagesArray = new ArrayList<>();
    }

    public void addMessageToArray(Message message) {
        messagesArray.add(message);
    }
    public void deleteMessage(String id) {
        int count = 0;

        for (Message message : messagesArray ) {
            count++;
            if (message.getId().equals(id)) {
                messagesArray.remove(count);
            }

        } 
    }


    public Message getMessageById (String id){
      Message foundMessage = null; 

        for (Message message : messagesArray ) {
            
            
            if (!message.getId().equals(id)) {
                System.out.println("besked kan ikke findes");
                
            } else {
                foundMessage = message;
            }
        
        } return foundMessage;
        
    }

    public void updateMessage (UUID id, String newText){
        for(Message message : messagesArray) {
            if(message.getId().equals(id)){
                message.setTextFromSender(newText);
                message.setTimestamp(java.time.LocalDateTime.now());
                break;
            }
        }
    }

    public List<Message> getMessages() {
        return messagesArray;
    }
    public void setMessages(List<Message> messages) {
        this.messagesArray = messages;
    }
}
