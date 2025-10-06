package dao;

import models.User;
import models.Recipe;
import models.Comment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In-memory database implementation using ArrayList and HashMap.
 * This class acts as a simple data store for the MVP without external database dependencies.
 * All data is lost when the application restarts.
 */
public class InMemoryDB {
    private static InMemoryDB instance;
    
    // Data storage
    private List<User> users;
    private List<Recipe> recipes;
    private List<Comment> comments;
    
    // ID counters for auto-increment
    private int nextUserId = 1;
    private int nextRecipeId = 1;
    private int nextCommentId = 1;
    
    /**
     * Private constructor for singleton pattern
     */
    private InMemoryDB() {
        users = new ArrayList<>();
        recipes = new ArrayList<>();
        comments = new ArrayList<>();
        initializeSampleData();
    }
    
    /**
     * Get singleton instance of the database
     */
    public static InMemoryDB getInstance() {
        if (instance == null) {
            instance = new InMemoryDB();
        }
        return instance;
    }
    
    // ========== USER OPERATIONS ==========
    
    /**
     * Add a new user to the database
     */
    public User addUser(String username, String email, String password, User.UserRole role) {
        User user = new User(nextUserId++, username, email, password, role);
        users.add(user);
        return user;
    }
    
    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }
    
    /**
     * Find user by username
     */
    public User findUserByUsername(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Find user by ID
     */
    public User findUserById(int id) {
        return users.stream()
                .filter(user -> user.getId() == id)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Update user information
     */
    public boolean updateUser(User updatedUser) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == updatedUser.getId()) {
                users.set(i, updatedUser);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Delete user by ID
     */
    public boolean deleteUser(int userId) {
        return users.removeIf(user -> user.getId() == userId);
    }
    
    // ========== RECIPE OPERATIONS ==========
    
    /**
     * Add a new recipe to the database
     */
    public Recipe addRecipe(String title, String ingredients, String instructions, 
                           int authorId, String authorName) {
        Recipe recipe = new Recipe(nextRecipeId++, title, ingredients, instructions, 
                                 authorId, authorName);
        recipes.add(recipe);
        return recipe;
    }
    
    /**
     * Get all recipes
     */
    public List<Recipe> getAllRecipes() {
        return new ArrayList<>(recipes);
    }
    
    /**
     * Get recipes by status
     */
    public List<Recipe> getRecipesByStatus(Recipe.RecipeStatus status) {
        return recipes.stream()
                .filter(recipe -> recipe.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    /**
     * Get recipes by author
     */
    public List<Recipe> getRecipesByAuthor(int authorId) {
        return recipes.stream()
                .filter(recipe -> recipe.getAuthorId() == authorId)
                .collect(Collectors.toList());
    }
    
    /**
     * Find recipe by ID
     */
    public Recipe findRecipeById(int id) {
        return recipes.stream()
                .filter(recipe -> recipe.getId() == id)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Update recipe information
     */
    public boolean updateRecipe(Recipe updatedRecipe) {
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).getId() == updatedRecipe.getId()) {
                recipes.set(i, updatedRecipe);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Delete recipe by ID
     */
    public boolean deleteRecipe(int recipeId) {
        return recipes.removeIf(recipe -> recipe.getId() == recipeId);
    }
    
    // ========== COMMENT OPERATIONS ==========
    
    /**
     * Add a new comment to the database
     */
    public Comment addComment(String text, int authorId, String authorName, int recipeId) {
        Comment comment = new Comment(nextCommentId++, text, authorId, authorName, recipeId);
        comments.add(comment);
        
        // Also add the comment to the recipe
        Recipe recipe = findRecipeById(recipeId);
        if (recipe != null) {
            recipe.addComment(comment);
        }
        
        return comment;
    }
    
    /**
     * Get all comments
     */
    public List<Comment> getAllComments() {
        return new ArrayList<>(comments);
    }
    
    /**
     * Get comments for a specific recipe
     */
    public List<Comment> getCommentsByRecipe(int recipeId) {
        return comments.stream()
                .filter(comment -> comment.getRecipeId() == recipeId)
                .collect(Collectors.toList());
    }
    
    /**
     * Get statistics for admin dashboard
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalUsers", users.size());
        stats.put("totalRecipes", recipes.size());
        stats.put("approvedRecipes", (int) recipes.stream().filter(r -> r.getStatus() == Recipe.RecipeStatus.APPROVED).count());
        stats.put("pendingRecipes", (int) recipes.stream().filter(r -> r.getStatus() == Recipe.RecipeStatus.PENDING).count());
        stats.put("rejectedRecipes", (int) recipes.stream().filter(r -> r.getStatus() == Recipe.RecipeStatus.REJECTED).count());
        stats.put("totalComments", comments.size());
        return stats;
    }
    
    /**
     * Initialize some sample data for testing purposes
     */
    private void initializeSampleData() {
        // Add sample admin user
        addUser("admin", "admin@recipe.com", "admin123", User.UserRole.ADMIN);
        
        // Add sample regular users
        addUser("john_chef", "john@email.com", "password123", User.UserRole.USER);
        addUser("mary_cook", "mary@email.com", "password123", User.UserRole.USER);
        addUser("bob_baker", "bob@email.com", "password123", User.UserRole.USER);
        
        // Add sample recipes
        Recipe recipe1 = addRecipe(
            "Classic Chocolate Chip Cookies",
            "2 cups flour\n1 cup butter\n3/4 cup brown sugar\n1/2 cup white sugar\n2 eggs\n2 tsp vanilla\n1 tsp baking soda\n1 tsp salt\n2 cups chocolate chips",
            "1. Preheat oven to 375°F\n2. Mix dry ingredients in a bowl\n3. Cream butter and sugars\n4. Add eggs and vanilla\n5. Combine wet and dry ingredients\n6. Fold in chocolate chips\n7. Drop onto baking sheet\n8. Bake for 9-11 minutes",
            2, "john_chef"
        );
        recipe1.setStatus(Recipe.RecipeStatus.APPROVED);
        recipe1.addRating(5);
        recipe1.addRating(4);
        recipe1.addRating(5);
        
        Recipe recipe2 = addRecipe(
            "Homemade Pizza Dough",
            "3 cups bread flour\n1 cup warm water\n2 tbsp olive oil\n1 tbsp sugar\n2 tsp active dry yeast\n1 tsp salt",
            "1. Dissolve yeast and sugar in warm water\n2. Let stand for 5 minutes until foamy\n3. Mix flour and salt in large bowl\n4. Add yeast mixture and olive oil\n5. Knead for 8-10 minutes\n6. Let rise for 1 hour\n7. Punch down and use for pizza",
            3, "mary_cook"
        );
        recipe2.setStatus(Recipe.RecipeStatus.APPROVED);
        recipe2.addRating(4);
        recipe2.addRating(5);
        
        addRecipe(
            "Banana Bread",
            "3 ripe bananas\n1/3 cup melted butter\n3/4 cup sugar\n1 egg\n1 tsp vanilla\n1 tsp baking soda\n1 1/3 cups flour\nPinch of salt",
            "1. Preheat oven to 350°F\n2. Mash bananas in mixing bowl\n3. Mix in melted butter\n4. Add sugar, egg, and vanilla\n5. Sprinkle baking soda and salt over mixture\n6. Add flour and mix\n7. Pour into greased loaf pan\n8. Bake for 60-65 minutes",
            4, "bob_baker"
        );
        // This recipe is pending approval
        
        // Add sample comments
        addComment("These cookies are amazing! My family loved them.", 3, "mary_cook", 1);
        addComment("Perfect recipe, followed exactly and they turned out great!", 4, "bob_baker", 1);
        addComment("Great base recipe for pizza. I added herbs to the dough.", 2, "john_chef", 2);
    }
}
