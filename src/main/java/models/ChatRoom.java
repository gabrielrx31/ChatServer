package models;

import java.util.ArrayList;
import java.util.List;

public class ChatRoom {
    private String roomName;
    private List<String> members;
    private int maxMembers;

    //Constructor
    public ChatRoom(String roomName){
        this.roomName = roomName;
        this.maxMembers = 10; // default max members
        this.members = new ArrayList<>();
    }

    //Constructor with maxMembers parameter
    public ChatRoom(String roomName, int maxMembers){
        this.roomName = roomName;
        this.maxMembers = maxMembers;
        this.members = new ArrayList<>();
    }

    //Add member to the chat room
    public boolean addMember(String username){
        if(members.size() >= maxMembers){
            return false; // Room is full
        }

        if(members.contains(username)){
            return false; // User already in the room
        }

        members.add(username);
        return true;
    }

    //Remove member from the chat room
    public boolean removeMember(String username){
        return members.remove(username);
    }

    //Check if a user is a member of the chat room
    public boolean isMember(String username){
        return members.contains(username);
    }

    //Check if the chat room is empty
    public boolean isEmpty(){
        return members.isEmpty();
    }

    //Get the number of members in the chat room
    public int getMemberCount(){
        return members.size();
    }
    
    //Check if the chat room is full
    public boolean isFull(){
        return members.size() >= maxMembers;
    }

    //Get the list of members in the chat room
    public List<String> getMembers() {
        return members;
    }

    //Getters and Setters
    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    //toString method for debugging
    @Override
    public String toString() {
        return "ChatRoom{" +
                "roomName='" + roomName + '\'' +
                ", members=" + members +
                ", maxMembers=" + maxMembers +
                '}';
    }


    
}
