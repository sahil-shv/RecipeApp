import ui.LoginFrame;
import utils.SwingUtils;

import javax.swing.*;

/**
 * Main entry point for the Recipe Sharing Platform MVP.
 * 
 * COMPILATION INSTRUCTIONS:
 * ========================
 * To compile and run this Java Swing application:
 * 
 * 1. Navigate to the project root directory (where src/ folder is located)
 * 
 * 2. Compile all Java files:
 *    javac -d . src/models/*.java src/services/*.java src/dao/*.java src/ui/*.java src/utils/*.java src/Main.java
 *    
 *    Or if the above doesn't work on your system:
 *    javac -d . src/Main.java src/models/*.java src/services/*.java src/dao/*.java src/ui/*.java src/utils/*.java
 * 
 * 3. Run the application:
 *    java Main
 * 
 * SYSTEM REQUIREMENTS:
 * ===================
 * - Java 17 or higher
 * - No external dependencies required (pure Java Swing)
 * 
 * SAMPLE USERS FOR TESTING:
 * =========================
 * Admin User:
 *   - Username: admin
 *   - Role: Admin
 *   - Capabilities: Manage users, approve/reject recipes, view statistics
 * 
 * Regular Users:
 *   - Username: john_chef (has approved recipes)
 *   - Username: mary_cook (has approved recipes)  
 *   - Username: bob_baker (has a pending recipe)
 *   - Role: User
 *   - Capabilities: Create recipes, view approved recipes, rate and comment
 * 
 * FEATURES IMPLEMENTED:
 * ====================
 * 1. User Authentication (simple username-based for MVP)
 * 2. Admin Dashboard:
 *    - User management (add, edit, delete users)
 *    - Recipe approval workflow (approve/reject pending recipes)
 *    - Platform statistics
 * 3. User Dashboard:
 *    - Browse approved recipes
 *    - Create and manage own recipes
 *    - Rate and comment on recipes
 *    - Profile management
 * 4. Recipe Management:
 *    - Create recipes (submitted as PENDING)
 *    - Edit pending recipes
 *    - View recipe details with ratings and comments
 * 5. Rating and Comment System:
 *    - 1-5 star rating system
 *    - Comment on approved recipes
 *    - Average rating calculation
 * 
 * ARCHITECTURE OVERVIEW:
 * =====================
 * - models/: Data models (User, Recipe, Comment)
 * - dao/: Data Access Object (InMemoryDB for storage)
 * - services/: Business logic layer (UserService, RecipeService, CommentService)
 * - ui/: Swing user interface components
 * - utils/: Helper utilities for Swing operations
 * 
 * DATA PERSISTENCE:
 * ================
 * This MVP uses in-memory storage. All data is lost when the application restarts.
 * Sample data is automatically loaded on startup for testing purposes.
 * 
 * EXTENDING THE APPLICATION:
 * =========================
 * To add database persistence:
 * 1. Replace InMemoryDB with a real database implementation
 * 2. Add JDBC dependencies and database connection logic
 * 3. Update DAO layer to use SQL queries instead of ArrayList operations
 * 
 * To add more features:
 * 1. Add new model classes in models/ package
 * 2. Create corresponding service classes for business logic
 * 3. Update UI components to support new features
 * 4. Follow the existing pattern of separation of concerns
 */
public class Main {
    
    /**
     * Application entry point.
     * Sets up the Swing environment and launches the login window.
     */
    public static void main(String[] args) {
        // Set system look and feel for better native appearance
        SwingUtils.setSystemLookAndFeel();
        
        // Run the GUI creation on the Event Dispatch Thread (EDT)
        // This is critical for thread safety in Swing applications
        SwingUtils.runOnEDT(() -> {
            try {
                // Create and show the login window
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
                
                System.out.println("Recipe Sharing Platform started successfully!");
                System.out.println("Sample users: admin, john_chef, mary_cook, bob_baker");
                System.out.println("Login as 'Admin' for admin features, 'User' for regular user features");
                
            } catch (Exception e) {
                // Handle any startup errors gracefully
                System.err.println("Failed to start application: " + e.getMessage());
                e.printStackTrace();
                
                // Show error dialog if possible
                JOptionPane.showMessageDialog(null, 
                    "Failed to start the Recipe Sharing Platform:\n" + e.getMessage(),
                    "Startup Error", 
                    JOptionPane.ERROR_MESSAGE);
                
                System.exit(1);
            }
        });
    }
}
