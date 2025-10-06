package services;

import dao.InMemoryDB;
import dao.FirebaseDB;
import models.Comment;
import models.Recipe;
import models.User;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service class for comment-related operations.
 * Handles comment creation and retrieval.
 * Now includes Firebase integration with local fallback.
 */
public class CommentService {
    private InMemoryDB localDatabase;
    private FirebaseDB firebaseDB;
    private SyncManager syncManager;
    
    public CommentService() {
        this.localDatabase = InMemoryDB.getInstance();
        this.firebaseDB = FirebaseDB.getInstance();
        this.syncManager = SyncManager.getInstance();
        
        // Register for comment change notifications
        syncManager.addCommentChangeListener(this::onCommentsChanged);
    }
    
    /**
     * Add a comment to a recipe
     */
    public Comment addComment(String text, User author, int recipeId) {
        // Basic validation
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment text cannot be empty");
        }
        if (author == null) {
            throw new IllegalArgumentException("Comment author cannot be null");
        }
        
        // Check if recipe exists and is approved
        Recipe recipe = localDatabase.findRecipeById(recipeId);
        if (recipe == null) {
            throw new IllegalArgumentException("Recipe not found");
        }
        if (recipe.getStatus() != Recipe.RecipeStatus.APPROVED) {
            throw new IllegalArgumentException("Can only comment on approved recipes");
        }
        
        // Create comment in local database
        Comment comment = localDatabase.addComment(text.trim(), author.getId(), author.getUsername(), recipeId);
        
        // Sync to Firebase and trigger immediate refresh
        syncCommentToFirebase(comment);
        
        // Force immediate sync to get latest data
        syncManager.forcSync();
        
        System.out.println("Comment added and synced to Firebase: " + author.getUsername() + " on recipe " + recipeId);
        
        return comment;
    }
    
    /**
     * Get all comments for a specific recipe
     */
    public List<Comment> getCommentsByRecipe(int recipeId) {
        // Try to get from Firebase first, fallback to local
        if (firebaseDB.isConnected()) {
            // For now, use local database as Firebase parsing is simplified
            // In production, you'd parse the Firebase response properly
        }
        
        return localDatabase.getCommentsByRecipe(recipeId);
    }
    
    /**
     * Get all comments (for admin)
     */
    public List<Comment> getAllComments() {
        return localDatabase.getAllComments();
    }
    
    /**
     * Check if user can comment on a recipe
     */
    public boolean canUserComment(User user, Recipe recipe) {
        if (user == null || recipe == null) {
            return false;
        }
        return recipe.getStatus() == Recipe.RecipeStatus.APPROVED;
    }
    
    /**
     * Delete a comment (admin only or comment author)
     */
    public boolean deleteComment(int commentId, User user) {
        Comment comment = findCommentById(commentId);
        if (comment == null) {
            return false;
        }
        
        // Check permissions
        if (user.getRole() != User.UserRole.ADMIN && comment.getAuthorId() != user.getId()) {
            throw new IllegalArgumentException("You can only delete your own comments");
        }
        
        // Delete from local database
        boolean success = deleteCommentFromLocal(commentId);
        
        if (success) {
            // Delete from Firebase
            deleteCommentFromFirebase(commentId);
            System.out.println("Comment deleted and removed from Firebase: " + commentId);
        }
        
        return success;
    }
    
    /**
     * Update a comment (comment author only)
     */
    public boolean updateComment(Comment comment, User user) {
        if (comment == null || user == null) {
            return false;
        }
        
        // Check permissions
        if (comment.getAuthorId() != user.getId()) {
            throw new IllegalArgumentException("You can only edit your own comments");
        }
        
        // Basic validation
        if (comment.getText() == null || comment.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Comment text cannot be empty");
        }
        
        // Update in local database
        boolean success = updateCommentInLocal(comment);
        
        if (success) {
            // Sync to Firebase
            syncCommentToFirebase(comment);
            System.out.println("Comment updated and synced to Firebase: " + comment.getId());
        }
        
        return success;
    }
    
    /**
     * Find comment by ID
     */
    public Comment findCommentById(int commentId) {
        List<Comment> allComments = localDatabase.getAllComments();
        return allComments.stream()
                .filter(comment -> comment.getId() == commentId)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get comments asynchronously from Firebase
     */
    public CompletableFuture<List<Comment>> getCommentsAsync(int recipeId) {
        return CompletableFuture.supplyAsync(() -> {
            if (firebaseDB.isConnected()) {
                String commentsJson = firebaseDB.get("comments");
                if (commentsJson != null && !commentsJson.equals("null")) {
                    // Parse JSON and filter by recipe ID
                    // For now, return local comments
                }
            }
            return getCommentsByRecipe(recipeId);
        });
    }
    
    /**
     * Sync a comment to Firebase
     */
    private void syncCommentToFirebase(Comment comment) {
        if (!firebaseDB.isConnected()) {
            System.out.println("Firebase not connected, comment stored locally only");
            return;
        }
        
        try {
            // Create a simple JSON representation
            String json = createCommentJson(comment);
            
            // Use async PUT to avoid blocking the UI
            CompletableFuture<String> future = firebaseDB.putAsync("comments/" + comment.getId(), json);
            future.thenAccept(response -> {
                if (response != null) {
                    System.out.println("Comment synced to Firebase: " + comment.getId());
                } else {
                    System.err.println("Failed to sync comment to Firebase: " + comment.getId());
                }
            }).exceptionally(throwable -> {
                System.err.println("Error syncing comment to Firebase: " + throwable.getMessage());
                return null;
            });
            
        } catch (Exception e) {
            System.err.println("Error syncing comment to Firebase: " + e.getMessage());
        }
    }
    
    /**
     * Delete a comment from Firebase
     */
    private void deleteCommentFromFirebase(int commentId) {
        if (!firebaseDB.isConnected()) {
            return;
        }
        
        try {
            firebaseDB.delete("comments/" + commentId);
        } catch (Exception e) {
            System.err.println("Error deleting comment from Firebase: " + e.getMessage());
        }
    }
    
    /**
     * Delete comment from local database
     */
    private boolean deleteCommentFromLocal(int commentId) {
        List<Comment> allComments = localDatabase.getAllComments();
        Comment toRemove = allComments.stream()
                .filter(comment -> comment.getId() == commentId)
                .findFirst()
                .orElse(null);
        
        if (toRemove != null) {
            allComments.remove(toRemove);
            
            // Also remove from recipe's comments list
            Recipe recipe = localDatabase.findRecipeById(toRemove.getRecipeId());
            if (recipe != null) {
                recipe.getComments().removeIf(comment -> comment.getId() == commentId);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Update comment in local database
     */
    private boolean updateCommentInLocal(Comment updatedComment) {
        List<Comment> allComments = localDatabase.getAllComments();
        
        for (int i = 0; i < allComments.size(); i++) {
            if (allComments.get(i).getId() == updatedComment.getId()) {
                allComments.set(i, updatedComment);
                
                // Also update in recipe's comments list
                Recipe recipe = localDatabase.findRecipeById(updatedComment.getRecipeId());
                if (recipe != null) {
                    List<Comment> recipeComments = recipe.getComments();
                    for (int j = 0; j < recipeComments.size(); j++) {
                        if (recipeComments.get(j).getId() == updatedComment.getId()) {
                            recipeComments.set(j, updatedComment);
                            break;
                        }
                    }
                }
                
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Create JSON representation of a comment
     * In production, use a proper JSON library like Gson
     */
    private String createCommentJson(Comment comment) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\":").append(comment.getId()).append(",");
        json.append("\"text\":").append(FirebaseDB.escapeJson(comment.getText())).append(",");
        json.append("\"authorId\":").append(comment.getAuthorId()).append(",");
        json.append("\"authorName\":").append(FirebaseDB.escapeJson(comment.getAuthorName())).append(",");
        json.append("\"recipeId\":").append(comment.getRecipeId()).append(",");
        json.append("\"createdAt\":").append(FirebaseDB.escapeJson(comment.getCreatedAt().toString()));
        json.append("}");
        return json.toString();
    }
    
    /**
     * Handle comment changes from sync manager
     */
    private void onCommentsChanged(List<Comment> updatedComments) {
        System.out.println("CommentService: Received comment updates from Firebase");
        // Here you could notify UI components that need to refresh
        // For now, just log the change
    }
    
    /**
     * Manually trigger sync with Firebase
     */
    public void forceSync() {
        syncManager.forcSync();
    }
    
    /**
     * Get recent comments (last 10)
     */
    public List<Comment> getRecentComments() {
        List<Comment> allComments = localDatabase.getAllComments();
        
        // Sort by creation date (newest first) and limit to 10
        return allComments.stream()
                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
                .limit(10)
                .toList();
    }
}