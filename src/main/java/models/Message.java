package models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Message {
  private UUID id;
  private UUID senderId;
  private UUID recieverId;
  private String textFromSender;
  private LocalDateTime timestamp;

  public Message(UUID id, UUID senderId, UUID recieverId, String textFromSender, LocalDateTime timeStamp) {
    this.id = id;
    this.recieverId = recieverId;
    this.senderId = senderId;
    this.textFromSender = textFromSender;
    this.timestamp = timeStamp;
  }

  

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getSenderId() {
    return senderId;
  }

  public void setSenderId(UUID senderId) {
    this.senderId = senderId;
  }

  public UUID getRecieverId() {
    return recieverId;
  }

  public void setRecieverId(UUID recieverId) {
    this.recieverId = recieverId;
  }

  public String getTextFromSender() {
    return textFromSender;
  }

  public void setTextFromSender(String textFromSender) {
    this.textFromSender = textFromSender;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  

  
}
