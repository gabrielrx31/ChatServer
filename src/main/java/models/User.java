package models;

import java.util.UUID;

public class User {
  private UUID id;
  private String userName;
  private String password;
  

  public User(UUID id, String userName, String password) {
    this.id = id;
    this.userName = userName;
    this.password = password;
  }


  public UUID getId() {
    return id;
  }


  public void setId(UUID id) {
    this.id = id;
  }


  public String getUserName() {
    return userName;
  }


  public void setUserName(String userName) {
    this.userName = userName;
  }


  public String getPassword() {
    return password;
  }


  public void setPassword(String password) {
    this.password = password;
  }

  
}
