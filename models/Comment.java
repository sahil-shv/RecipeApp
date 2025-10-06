package models;

import java.time.LocalDateTime;

/**
 * Comment model class representing a comment on a recipe.
 * Contains comment text, author information, and timestamp.
 */
public class Comment {
    private int id;
    private String text;
    private int authorId;
    private String authorName;
    private int recipeId;
    private LocalDateTime createdAt;
    
    /**
     * Constructor for creating a new comment
     */
    public Comment(int id, String text, int authorId, String authorName, int recipeId) {
        this.id = id;
        this.text = text;
        this.authorId = authorId;
        this.authorName = authorName;
        this.recipeId = recipeId;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public int getAuthorId() {
        return authorId;
    }
    
    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }
    
    public String getAuthorName() {
        return authorName;
    }
    
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
    
    public int getRecipeId() {
        return recipeId;
    }
    
    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return authorName + ": " + text;
    }
}
