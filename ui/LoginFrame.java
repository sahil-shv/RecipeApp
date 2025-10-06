package ui;

import services.UserService;
import models.User;
import utils.SwingUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Login window that allows users to select their role and enter username.
 * For MVP, we use simple username-based authentication without passwords.
 */
public class LoginFrame extends JFrame {
    private UserService userService;
    private JTextField usernameField;
    private JComboBox<String> roleComboBox;
    private JButton loginButton;
    
    public LoginFrame() {
        this.userService = new UserService();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        setTitle("Recipe Sharing Platform - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        SwingUtils.centerWindow(this);
    }
    
    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        usernameField = SwingUtils.createTextField(20);
        roleComboBox = new JComboBox<>(new String[]{"User", "Admin"});
    }
    
    /**
     * Setup the layout of components
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Main panel with padding
        JPanel mainPanel = SwingUtils.createPaddedPanel(new GridBagLayout(), 20);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Title
        JLabel titleLabel = SwingUtils.createLabel("Recipe Sharing Platform", JLabel.CENTER);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);
        
        // Username label
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(SwingUtils.createLabel("Username:", JLabel.RIGHT), gbc);
        
        // Username field
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(usernameField, gbc);
        
        // Role label
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(SwingUtils.createLabel("Login as:", JLabel.RIGHT), gbc);
        
        // Role combo box
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(roleComboBox, gbc);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        loginButton = SwingUtils.createButton("Login", this::handleLogin);
        JButton exitButton = SwingUtils.createButton("Exit", this::handleExit);
        
        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(buttonPanel, gbc);
        
        // Help text
        JLabel helpLabel = SwingUtils.createLabel(
            "<html><center>Sample users: admin, john_chef, mary_cook, bob_baker<br>" +
            "For MVP, just enter the username (no password required)</center></html>", 
            JLabel.CENTER);
        helpLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        mainPanel.add(helpLabel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        // Enter key in username field triggers login
        usernameField.addActionListener(event -> handleLogin());
        
        // Make login button default
        getRootPane().setDefaultButton(loginButton);
    }
    
    /**
     * Handle login attempt
     */
    private void handleLogin() {
        String username = usernameField.getText().trim();
        
        if (!SwingUtils.isValidString(username)) {
            SwingUtils.showError(this, "Please enter a username.");
            usernameField.requestFocus();
            return;
        }
        
        User user = userService.authenticateUser(username);
        if (user == null) {
            SwingUtils.showError(this, "User not found. Please check the username or create a new user.");
            usernameField.requestFocus();
            return;
        }
        
        // Check if the selected role matches the user's actual role
        String selectedRole = (String) roleComboBox.getSelectedItem();
        boolean isAdminLogin = "Admin".equals(selectedRole);
        boolean isUserAdmin = user.getRole() == User.UserRole.ADMIN;
        
        if (isAdminLogin && !isUserAdmin) {
            SwingUtils.showError(this, "This user is not an administrator.");
            return;
        }
        
        if (!isAdminLogin && isUserAdmin) {
            SwingUtils.showError(this, "Administrator users must login as Admin.");
            return;
        }
        
        // Login successful - open appropriate frame
        dispose();
        
        SwingUtils.runOnEDT(() -> {
            if (isUserAdmin) {
                new AdminFrame(user).setVisible(true);
            } else {
                new UserFrame(user).setVisible(true);
            }
        });
    }
    
    /**
     * Handle exit application
     */
    private void handleExit() {
        if (SwingUtils.showConfirmation(this, "Are you sure you want to exit?")) {
            System.exit(0);
        }
    }
}
