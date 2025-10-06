package models;

/**
 * User model class representing a user in the recipe sharing platform.
 * Contains basic user information and role (ADMIN or USER).
 */
public class User {
    private int id;
    private String username;
    private String email;
    private String password;
    private UserRole role;
    
    public enum UserRole {
        ADMIN, USER
    }
    
    /**
     * Constructor for creating a new user
     */
    public User(int id, String username, String email, String password, UserRole role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}
