package utils;

import javax.swing.*;
import java.awt.*;

/**
 * Utility class for common Swing operations and helpers.
 * Provides methods for running on EDT, input validation, and common UI patterns.
 */
public class SwingUtils {
    
    /**
     * Run code on the Event Dispatch Thread safely
     */
    public static void runOnEDT(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }
    
    /**
     * Center a window on the screen
     */
    public static void centerWindow(Window window) {
        window.setLocationRelativeTo(null);
    }
    
    /**
     * Create a simple label with specified text and alignment
     */
    public static JLabel createLabel(String text, int alignment) {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(alignment);
        return label;
    }
    
    /**
     * Create a text field with specified columns
     */
    public static JTextField createTextField(int columns) {
        JTextField field = new JTextField(columns);
        return field;
    }
    
    /**
     * Create a text area with specified rows and columns
     */
    public static JTextArea createTextArea(int rows, int columns) {
        JTextArea area = new JTextArea(rows, columns);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }
    
    /**
     * Create a button with specified text and action
     */
    public static JButton createButton(String text, Runnable action) {
        JButton button = new JButton(text);
        if (action != null) {
            button.addActionListener(event -> action.run());
        }
        return button;
    }
    
    /**
     * Show an error message dialog
     */
    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Show an information message dialog
     */
    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Show a confirmation dialog
     */
    public static boolean showConfirmation(Component parent, String message) {
        int result = JOptionPane.showConfirmDialog(parent, message, "Confirmation", 
                                                  JOptionPane.YES_NO_OPTION);
        return result == JOptionPane.YES_OPTION;
    }
    
    /**
     * Get input from user via dialog
     */
    public static String showInputDialog(Component parent, String message, String title) {
        return JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE);
    }
    
    /**
     * Validate that a string is not null or empty
     */
    public static boolean isValidString(String str) {
        return str != null && !str.trim().isEmpty();
    }
    
    /**
     * Validate that a rating is between 1 and 5
     */
    public static boolean isValidRating(int rating) {
        return rating >= 1 && rating <= 5;
    }
    
    /**
     * Create a scroll pane for a component
     */
    public static JScrollPane createScrollPane(Component component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }
    
    /**
     * Create a bordered panel with title
     */
    public static JPanel createTitledPanel(String title, LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }
    
    /**
     * Create a panel with padding
     */
    public static JPanel createPaddedPanel(LayoutManager layout, int padding) {
        JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        return panel;
    }
    
    /**
     * Set the look and feel to system default
     */
    public static void setSystemLookAndFeel() {
        try {
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            // Temporarily commented out due to compilation issues
            System.out.println("Using default look and feel");
        } catch (Exception e) {
            // Fall back to default if system L&F is not available
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }
    }
    
    /**
     * Create a table that's not editable
     */
    public static JTable createNonEditableTable() {
        return new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }
    
    /**
     * Format rating display
     */
    public static String formatRating(double rating) {
        if (rating == 0.0) {
            return "No ratings";
        }
        return String.format("★%.1f", rating);
    }
}
