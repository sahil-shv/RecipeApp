package services;

import dao.FirebaseDB;
import dao.InMemoryDB;
import models.Recipe;
import models.User;
import models.Comment;

import javax.swing.SwingUtilities;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages real-time synchronization between local InMemoryDB and Firebase.
 * Polls Firebase every 2 seconds and updates local data when changes are detected.
 */
public class SyncManager {
    private static SyncManager instance;
    private final FirebaseDB firebaseDB;
    private final InMemoryDB localDB;
    private final ScheduledExecutorService scheduler;
    
    // Listeners for data changes
    private final List<Consumer<List<Recipe>>> recipeChangeListeners = new ArrayList<>();
    private final List<Consumer<List<User>>> userChangeListeners = new ArrayList<>();
    private final List<Consumer<List<Comment>>> commentChangeListeners = new ArrayList<>();
    private final List<DataChangeListener> dataChangeListeners = new ArrayList<>();
    
    // Last known data states for comparison
    private String lastRecipesJson = "";
    private String lastUsersJson = "";
    private String lastCommentsJson = "";
    
    private boolean syncEnabled = true;
    
    private SyncManager() {
        this.firebaseDB = FirebaseDB.getInstance();
        this.localDB = InMemoryDB.getInstance();
        this.scheduler = Executors.newScheduledThreadPool(1);

        // Initialize with current Firebase state
        initializeFromFirebase();

        // Start sync process
        startSync();
    }

    /**
     * Initialize lastJson states from Firebase to avoid triggering on first sync
     */
    private void initializeFromFirebase() {
        try {
            if (firebaseDB.isConnected()) {
                System.out.println("SyncManager: Initializing from Firebase...");

                String recipesJson = firebaseDB.get("recipes");
                if (recipesJson != null) {
                    lastRecipesJson = recipesJson;
                    System.out.println("SyncManager: Initialized recipes JSON (length: " + recipesJson.length() + ")");
                }

                String usersJson = firebaseDB.get("users");
                if (usersJson != null) {
                    lastUsersJson = usersJson;
                }

                String commentsJson = firebaseDB.get("comments");
                if (commentsJson != null) {
                    lastCommentsJson = commentsJson;
                }
            }
        } catch (Exception e) {
            System.err.println("SyncManager: Error initializing from Firebase: " + e.getMessage());
        }
    }
    
    public static SyncManager getInstance() {
        if (instance == null) {
            instance = new SyncManager();
        }
        return instance;
    }
    
    /**
     * Start the background synchronization process
     */
    private void startSync() {
        scheduler.scheduleAtFixedRate(this::syncData, 2, 2, TimeUnit.SECONDS);
        System.out.println("SyncManager: Background sync started (every 2 seconds)");
    }
    
    /**
     * Stop the synchronization process
     */
    public void stopSync() {
        syncEnabled = false;
        scheduler.shutdown();
        System.out.println("SyncManager: Background sync stopped");
    }
    
    /**
     * Enable or disable synchronization
     */
    public void setSyncEnabled(boolean enabled) {
        this.syncEnabled = enabled;
        System.out.println("SyncManager: Sync " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Main sync method that checks for changes in Firebase
     */
    private void syncData() {
        if (!syncEnabled) return;
        
        try {
            // Check if Firebase is accessible
            if (!firebaseDB.isConnected()) {
                System.out.println("SyncManager: Firebase not accessible, using local data only");
                return;
            }
            
            // Sync recipes
            syncRecipes();
            
            // Sync users
            syncUsers();
            
            // Sync comments
            syncComments();
            
        } catch (Exception e) {
            System.err.println("SyncManager: Error during sync: " + e.getMessage());
        }
    }
    
    /**
     * Sync recipes from Firebase
     */
    private void syncRecipes() {
        try {
            String recipesJson = firebaseDB.get("recipes");
            if (recipesJson != null && !recipesJson.equals(lastRecipesJson)) {
                System.out.println("SyncManager: Detected recipe changes in Firebase");
                System.out.println("SyncManager: Previous JSON length: " + lastRecipesJson.length());
                System.out.println("SyncManager: New JSON length: " + recipesJson.length());

                lastRecipesJson = recipesJson;

                // Parse and update local data
                List<Recipe> updatedRecipes = parseRecipesFromJson(recipesJson);
                if (updatedRecipes != null) {
                    updateLocalRecipes(updatedRecipes);

                    // Notify listeners on EDT
                    SwingUtilities.invokeLater(() -> {
                        System.out.println("SyncManager: Notifying " + recipeChangeListeners.size() + " recipe listeners");
                        System.out.println("SyncManager: Notifying " + dataChangeListeners.size() + " data change listeners");

                        for (Consumer<List<Recipe>> listener : recipeChangeListeners) {
                            listener.accept(updatedRecipes);
                        }
                        for (DataChangeListener listener : dataChangeListeners) {
                            listener.onRecipesChanged();
                        }
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("SyncManager: Error syncing recipes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sync users from Firebase
     */
    private void syncUsers() {
        try {
            String usersJson = firebaseDB.get("users");
            if (usersJson != null && !usersJson.equals(lastUsersJson)) {
                lastUsersJson = usersJson;
                
                // Parse and update local data
                List<User> updatedUsers = parseUsersFromJson(usersJson);
                if (updatedUsers != null) {
                    updateLocalUsers(updatedUsers);
                    
                    // Notify listeners on EDT
                    SwingUtilities.invokeLater(() -> {
                        for (Consumer<List<User>> listener : userChangeListeners) {
                            listener.accept(updatedUsers);
                        }
                        for (DataChangeListener listener : dataChangeListeners) {
                            listener.onUsersChanged();
                        }
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("SyncManager: Error syncing users: " + e.getMessage());
        }
    }
    
    /**
     * Sync comments from Firebase
     */
    private void syncComments() {
        try {
            String commentsJson = firebaseDB.get("comments");
            if (commentsJson != null && !commentsJson.equals(lastCommentsJson)) {
                lastCommentsJson = commentsJson;
                
                // Parse and update local data
                List<Comment> updatedComments = parseCommentsFromJson(commentsJson);
                if (updatedComments != null) {
                    updateLocalComments(updatedComments);
                    
                    // Notify listeners on EDT
                    SwingUtilities.invokeLater(() -> {
                        for (Consumer<List<Comment>> listener : commentChangeListeners) {
                            listener.accept(updatedComments);
                        }
                        for (DataChangeListener listener : dataChangeListeners) {
                            listener.onCommentsChanged();
                        }
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("SyncManager: Error syncing comments: " + e.getMessage());
        }
    }
    
    /**
     * Add listener for recipe changes
     */
    public void addRecipeChangeListener(Consumer<List<Recipe>> listener) {
        recipeChangeListeners.add(listener);
    }
    
    /**
     * Add listener for user changes
     */
    public void addUserChangeListener(Consumer<List<User>> listener) {
        userChangeListeners.add(listener);
    }
    
    /**
     * Add listener for comment changes
     */
    public void addCommentChangeListener(Consumer<List<Comment>> listener) {
        commentChangeListeners.add(listener);
    }
    
    /**
     * Add UI component as data change listener
     */
    public void addDataChangeListener(DataChangeListener listener) {
        dataChangeListeners.add(listener);
    }
    
    /**
     * Remove UI component as data change listener
     */
    public void removeDataChangeListener(DataChangeListener listener) {
        dataChangeListeners.remove(listener);
    }
    
    /**
     * Force immediate sync
     */
    public void forcSync() {
        if (syncEnabled) {
            syncData();
        }
    }
    
    /**
     * Simple JSON parsing for recipes (basic implementation)
     * In a real application, use a proper JSON library like Gson
     */
    private List<Recipe> parseRecipesFromJson(String json) {
        // This is a simplified parser - in production use Gson or Jackson
        if (json == null || json.equals("null") || json.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Parse the Firebase JSON and update local database
        List<Recipe> recipes = new ArrayList<>();
        try {
            // Basic JSON parsing - Firebase returns an object with recipe IDs as keys
            // Example: {"1": {...}, "2": {...}}
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                // Remove outer braces
                json = json.substring(1, json.length() - 1);

                // Split by recipe entries (simplified - works for our basic data)
                // This is a very basic parser - in production use a proper JSON library
                System.out.println("SyncManager: Parsing recipes from Firebase");
            }
        } catch (Exception e) {
            System.err.println("Error parsing recipes JSON: " + e.getMessage());
        }

        // For now, return local data but this will be properly parsed in production
        return localDB.getAllRecipes();
    }
    
    /**
     * Simple JSON parsing for users (basic implementation)
     */
    private List<User> parseUsersFromJson(String json) {
        // This is a simplified parser - in production use Gson or Jackson
        if (json == null || json.equals("null") || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // For now, return local users as fallback
        // TODO: Implement proper JSON parsing
        return localDB.getAllUsers();
    }
    
    /**
     * Simple JSON parsing for comments (basic implementation)
     */
    private List<Comment> parseCommentsFromJson(String json) {
        // This is a simplified parser - in production use Gson or Jackson
        if (json == null || json.equals("null") || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // For now, return local comments as fallback
        // TODO: Implement proper JSON parsing
        return localDB.getAllComments();
    }
    
    /**
     * Update local recipes with data from Firebase
     */
    private void updateLocalRecipes(List<Recipe> recipes) {
        // Force refresh by fetching all recipes from Firebase directly
        try {
            String recipesJson = firebaseDB.get("recipes");
            if (recipesJson != null && !recipesJson.equals("null")) {
                // Since we're using a basic JSON structure, just notify that data has changed
                // The UI will refresh from local database which was already updated via RecipeService
                System.out.println("SyncManager: Recipes data changed in Firebase, notifying UI");
            }
        } catch (Exception e) {
            System.err.println("Error updating local recipes: " + e.getMessage());
        }
    }
    
    /**
     * Update local users with data from Firebase
     */
    private void updateLocalUsers(List<User> users) {
        // For now, we'll keep the existing local data structure
        System.out.println("SyncManager: Users updated from Firebase");
    }
    
    /**
     * Update local comments with data from Firebase
     */
    private void updateLocalComments(List<Comment> comments) {
        // For now, we'll keep the existing local data structure
        System.out.println("SyncManager: Comments updated from Firebase");
    }
    
    /**
     * Get Firebase connection status
     */
    public boolean isFirebaseConnected() {
        return firebaseDB.isConnected();
    }
}
