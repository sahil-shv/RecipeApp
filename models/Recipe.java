package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Recipe model class representing a recipe in the platform.
 * Contains recipe details, status, and associated comments/ratings.
 */
public class Recipe {
    private int id;
    private String title;
    private String ingredients;
    private String instructions;
    private int authorId;
    private String authorName;
    private RecipeStatus status;
    private LocalDateTime createdAt;
    private List<Comment> comments;
    private List<Integer> ratings;
    
    public enum RecipeStatus {
        PENDING, APPROVED, REJECTED
    }
    
    /**
     * Constructor for creating a new recipe
     */
    public Recipe(int id, String title, String ingredients, String instructions, 
                  int authorId, String authorName) {
        this.id = id;
        this.title = title;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.authorId = authorId;
        this.authorName = authorName;
        this.status = RecipeStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.comments = new ArrayList<>();
        this.ratings = new ArrayList<>();
    }
    
    /**
     * Calculate average rating for this recipe
     */
    public double getAverageRating() {
        if (ratings.isEmpty()) {
            return 0.0;
        }
        return ratings.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }
    
    /**
     * Add a rating to this recipe
     */
    public void addRating(int rating) {
        if (rating >= 1 && rating <= 5) {
            ratings.add(rating);
        }
    }
    
    /**
     * Add a comment to this recipe
     */
    public void addComment(Comment comment) {
        comments.add(comment);
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getIngredients() {
        return ingredients;
    }
    
    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }
    
    public String getInstructions() {
        return instructions;
    }
    
    public void setInstructions(String instructions) {
        this.instructions = instructions;
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
    
    public RecipeStatus getStatus() {
        return status;
    }
    
    public void setStatus(RecipeStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<Comment> getComments() {
        return comments;
    }
    
    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }
    
    public List<Integer> getRatings() {
        return ratings;
    }
    
    public void setRatings(List<Integer> ratings) {
        this.ratings = ratings;
    }
    
    @Override
    public String toString() {
        return title + " by " + authorName + " (★" + String.format("%.1f", getAverageRating()) + ")";
    }
}
