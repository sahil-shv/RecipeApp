package services;

/**
 * Interface for components that need to be notified of data changes.
 * Used to refresh UI components when data is updated in Firebase.
 */
public interface DataChangeListener {
    
    /**
     * Called when recipe data changes
     */
    default void onRecipesChanged() {
        // Default empty implementation
    }
    
    /**
     * Called when user data changes
     */
    default void onUsersChanged() {
        // Default empty implementation
    }
    
    /**
     * Called when comment data changes
     */
    default void onCommentsChanged() {
        // Default empty implementation
    }
    
    /**
     * Called when any data changes (general refresh)
     */
    default void onDataChanged() {
        onRecipesChanged();
        onUsersChanged();
        onCommentsChanged();
    }
}

