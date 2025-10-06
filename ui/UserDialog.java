package ui;

import models.User;
import services.UserService;
import utils.SwingUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for creating and editing user profiles.
 * Supports both admin user management and user profile editing.
 */
public class UserDialog extends JDialog {
    private User user;
    private UserService userService;
    private boolean confirmed = false;
    
    // UI Components
    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JComboBox<User.UserRole> roleComboBox;
    private JButton saveButton;
    private JButton cancelButton;
    
    public UserDialog(Window parent, String title, User user, UserService userService) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        
        this.user = user;
        this.userService = userService;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        populateFields();
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(parent);
    }
    
    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        usernameField = SwingUtils.createTextField(20);
        emailField = SwingUtils.createTextField(20);
        passwordField = new JPasswordField(20);
        confirmPasswordField = new JPasswordField(20);
        
        roleComboBox = new JComboBox<>(User.UserRole.values());
        
        saveButton = SwingUtils.createButton("Save", this::handleSave);
        cancelButton = SwingUtils.createButton("Cancel", this::handleCancel);
    }
    
    /**
     * Setup the layout of components
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Main form panel
        JPanel formPanel = SwingUtils.createPaddedPanel(new GridBagLayout(), 20);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Username
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(SwingUtils.createLabel("Username:", JLabel.RIGHT), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(usernameField, gbc);
        
        // Email
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(SwingUtils.createLabel("Email:", JLabel.RIGHT), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(emailField, gbc);
        
        // Password
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(SwingUtils.createLabel("Password:", JLabel.RIGHT), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(passwordField, gbc);
        
        // Confirm Password
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(SwingUtils.createLabel("Confirm Password:", JLabel.RIGHT), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(confirmPasswordField, gbc);
        
        // Role (only show for new users or admin editing)
        if (user == null || isAdminEditing()) {
            gbc.gridx = 0; gbc.gridy = 4; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
            formPanel.add(SwingUtils.createLabel("Role:", JLabel.RIGHT), gbc);
            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(roleComboBox, gbc);
        }
        
        add(formPanel, BorderLayout.CENTER);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Help text
        if (user != null) {
            JLabel helpLabel = SwingUtils.createLabel(
                "<html><center>Leave password fields empty to keep current password</center></html>",
                JLabel.CENTER);
            helpLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
            add(helpLabel, BorderLayout.NORTH);
        }
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        getRootPane().setDefaultButton(saveButton);
        
        // Enter key in any field triggers save
        usernameField.addActionListener(event -> handleSave());
        emailField.addActionListener(event -> handleSave());
        passwordField.addActionListener(event -> handleSave());
        confirmPasswordField.addActionListener(event -> handleSave());
    }
    
    /**
     * Populate fields with user data (for editing)
     */
    private void populateFields() {
        if (user != null) {
            usernameField.setText(user.getUsername());
            emailField.setText(user.getEmail());
            roleComboBox.setSelectedItem(user.getRole());
            
            // For existing users, username is not editable
            usernameField.setEditable(false);
        }
    }
    
    /**
     * Check if this is an admin editing a user
     */
    private boolean isAdminEditing() {
        // This is a simplified check - in a real app you'd pass the current user context
        return getTitle().contains("Admin") || getTitle().contains("Add User") || getTitle().contains("Edit User");
    }
    
    /**
     * Validate form input
     */
    private boolean validateInput() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        
        // Username validation
        if (!SwingUtils.isValidString(username)) {
            SwingUtils.showError(this, "Username cannot be empty.");
            usernameField.requestFocus();
            return false;
        }
        
        // Email validation
        if (!SwingUtils.isValidString(email)) {
            SwingUtils.showError(this, "Email cannot be empty.");
            emailField.requestFocus();
            return false;
        }
        
        if (!userService.isValidEmail(email)) {
            SwingUtils.showError(this, "Please enter a valid email address.");
            emailField.requestFocus();
            return false;
        }
        
        // Password validation (only for new users or when password is being changed)
        if (user == null || !password.isEmpty()) {
            if (password.isEmpty()) {
                SwingUtils.showError(this, "Password cannot be empty.");
                passwordField.requestFocus();
                return false;
            }
            
            if (password.length() < 6) {
                SwingUtils.showError(this, "Password must be at least 6 characters long.");
                passwordField.requestFocus();
                return false;
            }
            
            if (!password.equals(confirmPassword)) {
                SwingUtils.showError(this, "Passwords do not match.");
                confirmPasswordField.requestFocus();
                return false;
            }
        }
        
        return true;
    }
    
    // ========== EVENT HANDLERS ==========
    
    private void handleSave() {
        if (!validateInput()) {
            return;
        }
        
        try {
            String username = usernameField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());
            User.UserRole role = (User.UserRole) roleComboBox.getSelectedItem();
            
            if (user == null) {
                // Creating new user
                if (role == null) {
                    role = User.UserRole.USER; // Default role
                }
                userService.createUser(username, email, password, role);
                SwingUtils.showInfo(this, "User created successfully!");
            } else {
                // Updating existing user
                user.setEmail(email);
                
                // Update password only if provided
                if (!password.isEmpty()) {
                    user.setPassword(password);
                }
                
                // Update role only if admin is editing
                if (isAdminEditing() && role != null) {
                    user.setRole(role);
                }
                
                userService.updateUser(user);
                SwingUtils.showInfo(this, "User updated successfully!");
            }
            
            confirmed = true;
            dispose();
            
        } catch (IllegalArgumentException e) {
            SwingUtils.showError(this, e.getMessage());
        } catch (Exception e) {
            SwingUtils.showError(this, "Failed to save user: " + e.getMessage());
        }
    }
    
    private void handleCancel() {
        dispose();
    }
    
    /**
     * Check if the dialog was confirmed (saved)
     */
    public boolean isConfirmed() {
        return confirmed;
    }
}
