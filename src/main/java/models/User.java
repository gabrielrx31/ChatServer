package models;

public class User {
  private String id;
  private String userName;
  private String password;
  
  public User(String id, String userName, String password) {
    this.id = id;
    this.userName = userName;
    this.password = password;
    
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

}
