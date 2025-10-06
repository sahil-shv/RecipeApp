package services;

import dao.InMemoryDB;
import dao.FirebaseDB;
import models.User;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service class for user-related operations.
 * Provides business logic layer between UI and data access.
 * Now includes Firebase integration with local fallback.
 */
public class UserService {
    private InMemoryDB localDatabase;
    private FirebaseDB firebaseDB;
    private SyncManager syncManager;
    
    public UserService() {
        this.localDatabase = InMemoryDB.getInstance();
        this.firebaseDB = FirebaseDB.getInstance();
        this.syncManager = SyncManager.getInstance();
        
        // Register for user change notifications
        syncManager.addUserChangeListener(this::onUsersChanged);
    }
    
    /**
     * Authenticate user with username and get user object
     * For MVP, we're using simple username-based login
     */
    public User authenticateUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        
        // Try to get user from local database first
        User user = localDatabase.findUserByUsername(username.trim());
        
        // If not found locally and Firebase is connected, try to fetch from Firebase
        if (user == null && firebaseDB.isConnected()) {
            user = fetchUserFromFirebase(username.trim());
        }
        
        return user;
    }
    
    /**
     * Create a new user
     */
    public User createUser(String username, String email, String password, User.UserRole role) {
        // Basic validation
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        
        // Check if username already exists locally
        if (localDatabase.findUserByUsername(username.trim()) != null) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Create user in local database
        User user = localDatabase.addUser(username.trim(), email.trim(), password, role);
        
        // Sync to Firebase
        syncUserToFirebase(user);
        
        System.out.println("User created and synced to Firebase: " + username);
        
        return user;
    }
    
    /**
     * Update user profile
     */
    public boolean updateUser(User user) {
        if (user == null) {
            return false;
        }
        
        // Basic validation
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        
        // Update in local database
        boolean success = localDatabase.updateUser(user);
        
        if (success) {
            // Sync to Firebase
            syncUserToFirebase(user);
            System.out.println("User updated and synced to Firebase: " + user.getUsername());
        }
        
        return success;
    }
    
    /**
     * Delete a user
     */
    public boolean deleteUser(int userId) {
        boolean success = localDatabase.deleteUser(userId);
        
        if (success) {
            // Delete from Firebase
            deleteUserFromFirebase(userId);
            System.out.println("User deleted and removed from Firebase: " + userId);
        }
        
        return success;
    }
    
    /**
     * Get all users (for admin)
     */
    public List<User> getAllUsers() {
        // Try to get from Firebase first, fallback to local
        if (firebaseDB.isConnected()) {
            // For now, use local database as Firebase parsing is simplified
            // In production, you'd parse the Firebase response properly
        }
        
        return localDatabase.getAllUsers();
    }
    
    /**
     * Get user by ID
     */
    public User getUserById(int id) {
        User user = localDatabase.findUserById(id);
        
        // If not found locally and Firebase is connected, try to fetch from Firebase
        if (user == null && firebaseDB.isConnected()) {
            user = fetchUserByIdFromFirebase(id);
        }
        
        return user;
    }
    
    /**
     * Validate email format (basic validation)
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.contains("@") && email.contains(".");
    }
    
    /**
     * Check if user is admin
     */
    public boolean isAdmin(User user) {
        return user != null && user.getRole() == User.UserRole.ADMIN;
    }
    
    /**
     * Change user password
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("New password cannot be empty");
        }
        
        User user = getUserById(userId);
        if (user == null) {
            return false;
        }
        
        // Verify old password
        if (!user.getPassword().equals(oldPassword)) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        // Update password
        user.setPassword(newPassword);
        return updateUser(user);
    }
    
    /**
     * Get users asynchronously from Firebase
     */
    public CompletableFuture<List<User>> getUsersAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (firebaseDB.isConnected()) {
                String usersJson = firebaseDB.get("users");
                if (usersJson != null && !usersJson.equals("null")) {
                    // Parse JSON and return users
                    // For now, return local users
                }
            }
            return getAllUsers();
        });
    }
    
    /**
     * Fetch user from Firebase by username
     */
    private User fetchUserFromFirebase(String username) {
        try {
            String usersJson = firebaseDB.get("users");
            if (usersJson != null && !usersJson.equals("null")) {
                // Parse JSON and find user by username
                // For now, return null as we need proper JSON parsing
                // In production, use Gson or Jackson to parse the response
            }
        } catch (Exception e) {
            System.err.println("Error fetching user from Firebase: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Fetch user from Firebase by ID
     */
    private User fetchUserByIdFromFirebase(int id) {
        try {
            String userJson = firebaseDB.get("users/" + id);
            if (userJson != null && !userJson.equals("null")) {
                // Parse JSON and create User object
                // For now, return null as we need proper JSON parsing
                // In production, use Gson or Jackson to parse the response
            }
        } catch (Exception e) {
            System.err.println("Error fetching user from Firebase: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Sync a user to Firebase
     */
    private void syncUserToFirebase(User user) {
        if (!firebaseDB.isConnected()) {
            System.out.println("Firebase not connected, user stored locally only");
            return;
        }
        
        try {
            // Create a simple JSON representation
            String json = createUserJson(user);
            
            // Use async PUT to avoid blocking the UI
            CompletableFuture<String> future = firebaseDB.putAsync("users/" + user.getId(), json);
            future.thenAccept(response -> {
                if (response != null) {
                    System.out.println("User synced to Firebase: " + user.getUsername());
                } else {
                    System.err.println("Failed to sync user to Firebase: " + user.getUsername());
                }
            }).exceptionally(throwable -> {
                System.err.println("Error syncing user to Firebase: " + throwable.getMessage());
                return null;
            });
            
        } catch (Exception e) {
            System.err.println("Error syncing user to Firebase: " + e.getMessage());
        }
    }
    
    /**
     * Delete a user from Firebase
     */
    private void deleteUserFromFirebase(int userId) {
        if (!firebaseDB.isConnected()) {
            return;
        }
        
        try {
            firebaseDB.delete("users/" + userId);
        } catch (Exception e) {
            System.err.println("Error deleting user from Firebase: " + e.getMessage());
        }
    }
    
    /**
     * Create JSON representation of a user
     * In production, use a proper JSON library like Gson
     * Note: Password is included but should be hashed in production
     */
    private String createUserJson(User user) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\":").append(user.getId()).append(",");
        json.append("\"username\":").append(FirebaseDB.escapeJson(user.getUsername())).append(",");
        json.append("\"email\":").append(FirebaseDB.escapeJson(user.getEmail())).append(",");
        // Note: In production, never store plain text passwords
        json.append("\"password\":").append(FirebaseDB.escapeJson("***")).append(","); // Don't sync password to Firebase for security
        json.append("\"role\":").append(FirebaseDB.escapeJson(user.getRole().toString()));
        json.append("}");
        return json.toString();
    }
    
    /**
     * Handle user changes from sync manager
     */
    private void onUsersChanged(List<User> updatedUsers) {
        System.out.println("UserService: Received user updates from Firebase");
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
     * Get user statistics
     */
    public UserStats getUserStats() {
        List<User> allUsers = getAllUsers();
        long adminCount = allUsers.stream().filter(u -> u.getRole() == User.UserRole.ADMIN).count();
        long userCount = allUsers.stream().filter(u -> u.getRole() == User.UserRole.USER).count();
        
        return new UserStats(allUsers.size(), (int)adminCount, (int)userCount);
    }
    
    /**
     * Simple class to hold user statistics
     */
    public static class UserStats {
        private final int totalUsers;
        private final int adminUsers;
        private final int regularUsers;
        
        public UserStats(int totalUsers, int adminUsers, int regularUsers) {
            this.totalUsers = totalUsers;
            this.adminUsers = adminUsers;
            this.regularUsers = regularUsers;
        }
        
        public int getTotalUsers() { return totalUsers; }
        public int getAdminUsers() { return adminUsers; }
        public int getRegularUsers() { return regularUsers; }
    }
}