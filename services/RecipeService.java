package services;

import dao.InMemoryDB;
import dao.FirebaseDB;
import models.Recipe;
import models.User;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service class for recipe-related operations.
 * Handles recipe creation, approval workflow, and filtering.
 * Now includes Firebase integration with local fallback.
 */
public class RecipeService {
    private InMemoryDB localDatabase;
    private FirebaseDB firebaseDB;
    private SyncManager syncManager;
    
    public RecipeService() {
        this.localDatabase = InMemoryDB.getInstance();
        this.firebaseDB = FirebaseDB.getInstance();
        this.syncManager = SyncManager.getInstance();
        
        // Register for recipe change notifications
        syncManager.addRecipeChangeListener(this::onRecipesChanged);
    }
    
    /**
     * Create a new recipe (starts as PENDING)
     */
    public Recipe createRecipe(String title, String ingredients, String instructions, User author) {
        // Basic validation
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipe title cannot be empty");
        }
        if (ingredients == null || ingredients.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingredients cannot be empty");
        }
        if (instructions == null || instructions.trim().isEmpty()) {
            throw new IllegalArgumentException("Instructions cannot be empty");
        }
        if (author == null) {
            throw new IllegalArgumentException("Author cannot be null");
        }
        
        // Create recipe in local database first
        Recipe recipe = localDatabase.addRecipe(title.trim(), ingredients.trim(), instructions.trim(), 
                                               author.getId(), author.getUsername());
        
        // Sync to Firebase and trigger immediate refresh
        syncRecipeToFirebase(recipe);
        
        // Force immediate sync to get latest data
        syncManager.forcSync();
        
        return recipe;
    }
    
    /**
     * Update an existing recipe
     */
    public boolean updateRecipe(Recipe recipe) {
        if (recipe == null) {
            return false;
        }
        
        // Basic validation
        if (recipe.getTitle() == null || recipe.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Recipe title cannot be empty");
        }
        if (recipe.getIngredients() == null || recipe.getIngredients().trim().isEmpty()) {
            throw new IllegalArgumentException("Ingredients cannot be empty");
        }
        if (recipe.getInstructions() == null || recipe.getInstructions().trim().isEmpty()) {
            throw new IllegalArgumentException("Instructions cannot be empty");
        }
        
        // Update in local database
        boolean success = localDatabase.updateRecipe(recipe);
        
        if (success) {
            // Sync to Firebase and trigger immediate refresh
            syncRecipeToFirebase(recipe);
            
            // Force immediate sync to get latest data
            syncManager.forcSync();
        }
        
        return success;
    }
    
    /**
     * Get all approved recipes (for regular users)
     */
    public List<Recipe> getApprovedRecipes() {
        // Try to get from Firebase first, fallback to local
        if (firebaseDB.isConnected()) {
            // For now, use local database as Firebase parsing is simplified
            // In production, you'd parse the Firebase response properly
        }
        
        return localDatabase.getRecipesByStatus(Recipe.RecipeStatus.APPROVED);
    }
    
    /**
     * Get all pending recipes (for admin approval)
     */
    public List<Recipe> getPendingRecipes() {
        return localDatabase.getRecipesByStatus(Recipe.RecipeStatus.PENDING);
    }
    
    /**
     * Get all recipes by a specific author
     */
    public List<Recipe> getRecipesByAuthor(int authorId) {
        return localDatabase.getRecipesByAuthor(authorId);
    }
    
    /**
     * Get all recipes (for admin)
     */
    public List<Recipe> getAllRecipes() {
        return localDatabase.getAllRecipes();
    }
    
    /**
     * Approve a recipe (admin only)
     */
    public boolean approveRecipe(int recipeId) {
        Recipe recipe = localDatabase.findRecipeById(recipeId);
        if (recipe != null) {
            recipe.setStatus(Recipe.RecipeStatus.APPROVED);
            boolean success = localDatabase.updateRecipe(recipe);
            
            if (success) {
                // Sync to Firebase and trigger immediate refresh
                syncRecipeToFirebase(recipe);
                
                // Force immediate sync to get latest data
                syncManager.forcSync();
                
                System.out.println("Recipe approved and synced to Firebase: " + recipe.getTitle());
            }
            
            return success;
        }
        return false;
    }
    
    /**
     * Reject a recipe (admin only)
     */
    public boolean rejectRecipe(int recipeId) {
        Recipe recipe = localDatabase.findRecipeById(recipeId);
        if (recipe != null) {
            recipe.setStatus(Recipe.RecipeStatus.REJECTED);
            boolean success = localDatabase.updateRecipe(recipe);
            
            if (success) {
                // Sync to Firebase and trigger immediate refresh
                syncRecipeToFirebase(recipe);
                
                // Force immediate sync to get latest data
                syncManager.forcSync();
                
                System.out.println("Recipe rejected and synced to Firebase: " + recipe.getTitle());
            }
            
            return success;
        }
        return false;
    }
    
    /**
     * Add rating to a recipe
     */
    public boolean addRating(int recipeId, int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        Recipe recipe = localDatabase.findRecipeById(recipeId);
        if (recipe != null && recipe.getStatus() == Recipe.RecipeStatus.APPROVED) {
            recipe.addRating(rating);
            boolean success = localDatabase.updateRecipe(recipe);
            
            if (success) {
                // Sync to Firebase and trigger immediate refresh
                syncRecipeToFirebase(recipe);
                
                // Force immediate sync to get latest data
                syncManager.forcSync();
                
                System.out.println("Rating added and synced to Firebase: " + rating + " stars for " + recipe.getTitle());
            }
            
            return success;
        }
        return false;
    }
    
    /**
     * Get recipe by ID
     */
    public Recipe getRecipeById(int id) {
        return localDatabase.findRecipeById(id);
    }
    
    /**
     * Delete a recipe
     */
    public boolean deleteRecipe(int recipeId) {
        boolean success = localDatabase.deleteRecipe(recipeId);
        
        if (success) {
            // Delete from Firebase
            deleteRecipeFromFirebase(recipeId);
            System.out.println("Recipe deleted and removed from Firebase: " + recipeId);
        }
        
        return success;
    }
    
    /**
     * Check if user can edit this recipe (author or admin)
     */
    public boolean canUserEditRecipe(User user, Recipe recipe) {
        if (user == null || recipe == null) {
            return false;
        }
        return user.getRole() == User.UserRole.ADMIN || user.getId() == recipe.getAuthorId();
    }
    
    /**
     * Sync a recipe to Firebase
     */
    private void syncRecipeToFirebase(Recipe recipe) {
        if (!firebaseDB.isConnected()) {
            System.out.println("Firebase not connected, recipe stored locally only");
            return;
        }
        
        try {
            // Create a simple JSON representation
            String json = createRecipeJson(recipe);
            
            // Use async PUT to avoid blocking the UI
            CompletableFuture<String> future = firebaseDB.putAsync("recipes/" + recipe.getId(), json);
            future.thenAccept(response -> {
                if (response != null) {
                    System.out.println("Recipe synced to Firebase: " + recipe.getTitle());
                } else {
                    System.err.println("Failed to sync recipe to Firebase: " + recipe.getTitle());
                }
            }).exceptionally(throwable -> {
                System.err.println("Error syncing recipe to Firebase: " + throwable.getMessage());
                return null;
            });
            
        } catch (Exception e) {
            System.err.println("Error syncing recipe to Firebase: " + e.getMessage());
        }
    }
    
    /**
     * Delete a recipe from Firebase
     */
    private void deleteRecipeFromFirebase(int recipeId) {
        if (!firebaseDB.isConnected()) {
            return;
        }
        
        try {
            firebaseDB.delete("recipes/" + recipeId);
        } catch (Exception e) {
            System.err.println("Error deleting recipe from Firebase: " + e.getMessage());
        }
    }
    
    /**
     * Create JSON representation of a recipe
     * In production, use a proper JSON library like Gson
     */
    private String createRecipeJson(Recipe recipe) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\":").append(recipe.getId()).append(",");
        json.append("\"title\":").append(FirebaseDB.escapeJson(recipe.getTitle())).append(",");
        json.append("\"ingredients\":").append(FirebaseDB.escapeJson(recipe.getIngredients())).append(",");
        json.append("\"instructions\":").append(FirebaseDB.escapeJson(recipe.getInstructions())).append(",");
        json.append("\"authorId\":").append(recipe.getAuthorId()).append(",");
        json.append("\"authorName\":").append(FirebaseDB.escapeJson(recipe.getAuthorName())).append(",");
        json.append("\"status\":").append(FirebaseDB.escapeJson(recipe.getStatus().toString())).append(",");
        json.append("\"createdAt\":").append(FirebaseDB.escapeJson(recipe.getCreatedAt().toString())).append(",");
        json.append("\"averageRating\":").append(recipe.getAverageRating()).append(",");
        json.append("\"ratingCount\":").append(recipe.getRatings().size());
        json.append("}");
        return json.toString();
    }
    
    /**
     * Handle recipe changes from sync manager
     */
    private void onRecipesChanged(List<Recipe> updatedRecipes) {
        System.out.println("RecipeService: Received recipe updates from Firebase");
        // Here you could notify UI components that need to refresh
        // For now, just log the change
    }
    
    /**
     * Get recipes asynchronously from Firebase
     */
    public CompletableFuture<List<Recipe>> getRecipesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (firebaseDB.isConnected()) {
                String recipesJson = firebaseDB.get("recipes");
                if (recipesJson != null && !recipesJson.equals("null")) {
                    // Parse JSON and return recipes
                    // For now, return local recipes
                }
            }
            return getAllRecipes();
        });
    }
    
    /**
     * Manually trigger sync with Firebase
     */
    public void forceSync() {
        syncManager.forcSync();
    }
}