package ui;

import models.Recipe;
import models.User;
import services.RecipeService;
import services.UserService;
import services.CommentService;
import services.SyncManager;
import services.DataChangeListener;
import utils.SwingUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * User dashboard for regular users to view recipes, create new recipes, and manage profile.
 * Provides access to approved recipes, recipe creation, and profile management.
 */
public class UserFrame extends JFrame implements DataChangeListener {
    private User currentUser;
    private UserService userService;
    private RecipeService recipeService;
    private CommentService commentService;
    private SyncManager syncManager;
    
    // UI Components
    private JTabbedPane tabbedPane;
    private JTable recipesTable;
    private JTable myRecipesTable;
    private DefaultTableModel recipesTableModel;
    private DefaultTableModel myRecipesTableModel;
    
    public UserFrame(User user) {
        this.currentUser = user;
        this.userService = new UserService();
        this.recipeService = new RecipeService();
        this.commentService = new CommentService();
        this.syncManager = SyncManager.getInstance();
        
        // Register for data change notifications
        syncManager.addDataChangeListener(this);
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        refreshData();
        
        setTitle("Recipe Platform - User Dashboard (" + user.getUsername() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        SwingUtils.centerWindow(this);
    }
    
    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        tabbedPane = new JTabbedPane();
        
        // All recipes table
        String[] recipeColumns = {"ID", "Title", "Author", "Rating", "Comments"};
        recipesTableModel = new DefaultTableModel(recipeColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        recipesTable = new JTable(recipesTableModel);
        recipesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // My recipes table
        String[] myRecipeColumns = {"ID", "Title", "Status", "Rating", "Comments"};
        myRecipesTableModel = new DefaultTableModel(myRecipeColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        myRecipesTable = new JTable(myRecipesTableModel);
        myRecipesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
    
    /**
     * Setup the layout of components
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Create tabs
        createBrowseRecipesTab();
        createMyRecipesTab();
        createProfileTab();
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Menu bar
        createMenuBar();
    }
    
    /**
     * Create menu bar
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        refreshItem.addActionListener(event -> refreshData());
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(event -> handleLogout());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(event -> handleExit());
        
        fileMenu.add(refreshItem);
        fileMenu.addSeparator();
        fileMenu.add(logoutItem);
        fileMenu.add(exitItem);
        
        JMenu recipeMenu = new JMenu("Recipe");
        JMenuItem newRecipeItem = new JMenuItem("New Recipe");
        newRecipeItem.addActionListener(event -> handleNewRecipe());
        recipeMenu.add(newRecipeItem);
        
        menuBar.add(fileMenu);
        menuBar.add(recipeMenu);
        setJMenuBar(menuBar);
    }
    
    /**
     * Create browse recipes tab
     */
    private void createBrowseRecipesTab() {
        JPanel recipesPanel = new JPanel(new BorderLayout());
        
        // Info label
        JLabel infoLabel = SwingUtils.createLabel(
            "Browse all approved recipes. Double-click to view details, rate, and comment.", 
            JLabel.CENTER);
        recipesPanel.add(infoLabel, BorderLayout.NORTH);
        
        // Recipes table with scroll pane
        JScrollPane recipesScrollPane = SwingUtils.createScrollPane(recipesTable);
        recipesPanel.add(recipesScrollPane, BorderLayout.CENTER);
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout());
        JButton viewRecipeButton = SwingUtils.createButton("View Recipe", this::handleViewRecipe);
        JButton refreshButton = SwingUtils.createButton("Refresh", this::refreshApprovedRecipes);
        
        buttonsPanel.add(viewRecipeButton);
        buttonsPanel.add(refreshButton);
        
        recipesPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        tabbedPane.addTab("Browse Recipes", recipesPanel);
    }
    
    /**
     * Create my recipes tab
     */
    private void createMyRecipesTab() {
        JPanel myRecipesPanel = new JPanel(new BorderLayout());
        
        // Info label
        JLabel infoLabel = SwingUtils.createLabel(
            "Manage your own recipes. Create new ones or edit existing ones.", 
            JLabel.CENTER);
        myRecipesPanel.add(infoLabel, BorderLayout.NORTH);
        
        // My recipes table with scroll pane
        JScrollPane myRecipesScrollPane = SwingUtils.createScrollPane(myRecipesTable);
        myRecipesPanel.add(myRecipesScrollPane, BorderLayout.CENTER);
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout());
        JButton newRecipeButton = SwingUtils.createButton("New Recipe", this::handleNewRecipe);
        JButton editRecipeButton = SwingUtils.createButton("Edit Recipe", this::handleEditRecipe);
        JButton viewMyRecipeButton = SwingUtils.createButton("View Recipe", this::handleViewMyRecipe);
        JButton deleteRecipeButton = SwingUtils.createButton("Delete Recipe", this::handleDeleteRecipe);
        JButton refreshButton = SwingUtils.createButton("Refresh", this::refreshMyRecipes);
        
        buttonsPanel.add(newRecipeButton);
        buttonsPanel.add(editRecipeButton);
        buttonsPanel.add(viewMyRecipeButton);
        buttonsPanel.add(deleteRecipeButton);
        buttonsPanel.add(refreshButton);
        
        myRecipesPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        tabbedPane.addTab("My Recipes", myRecipesPanel);
    }
    
    /**
     * Create profile management tab
     */
    private void createProfileTab() {
        JPanel profilePanel = SwingUtils.createPaddedPanel(new GridBagLayout(), 20);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Title
        JLabel titleLabel = SwingUtils.createLabel("Profile Information", JLabel.CENTER);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        profilePanel.add(titleLabel, gbc);
        
        // Username
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST;
        profilePanel.add(SwingUtils.createLabel("Username:", JLabel.RIGHT), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        profilePanel.add(SwingUtils.createLabel(currentUser.getUsername(), JLabel.LEFT), gbc);
        
        // Email
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        profilePanel.add(SwingUtils.createLabel("Email:", JLabel.RIGHT), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        profilePanel.add(SwingUtils.createLabel(currentUser.getEmail(), JLabel.LEFT), gbc);
        
        // Role
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        profilePanel.add(SwingUtils.createLabel("Role:", JLabel.RIGHT), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        profilePanel.add(SwingUtils.createLabel(currentUser.getRole().toString(), JLabel.LEFT), gbc);
        
        // Edit button
        JButton editProfileButton = SwingUtils.createButton("Edit Profile", this::handleEditProfile);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        profilePanel.add(editProfileButton, gbc);
        
        tabbedPane.addTab("Profile", profilePanel);
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        // Double-click to view recipe in browse tab
        recipesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleViewRecipe();
                }
            }
        });
        
        // Double-click to edit recipe in my recipes tab
        myRecipesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleEditRecipe();
                }
            }
        });
    }
    
    /**
     * Refresh all data
     */
    private void refreshData() {
        refreshApprovedRecipes();
        refreshMyRecipes();
    }
    
    /**
     * Refresh approved recipes table
     */
    private void refreshApprovedRecipes() {
        recipesTableModel.setRowCount(0);
        List<Recipe> recipes = recipeService.getApprovedRecipes();
        
        for (Recipe recipe : recipes) {
            Object[] row = {
                recipe.getId(),
                recipe.getTitle(),
                recipe.getAuthorName(),
                SwingUtils.formatRating(recipe.getAverageRating()),
                recipe.getComments().size()
            };
            recipesTableModel.addRow(row);
        }
    }
    
    /**
     * Refresh my recipes table
     */
    private void refreshMyRecipes() {
        myRecipesTableModel.setRowCount(0);
        List<Recipe> myRecipes = recipeService.getRecipesByAuthor(currentUser.getId());
        
        for (Recipe recipe : myRecipes) {
            Object[] row = {
                recipe.getId(),
                recipe.getTitle(),
                recipe.getStatus(),
                SwingUtils.formatRating(recipe.getAverageRating()),
                recipe.getComments().size()
            };
            myRecipesTableModel.addRow(row);
        }
    }
    
    // ========== EVENT HANDLERS ==========
    
    private void handleViewRecipe() {
        int selectedRow = recipesTable.getSelectedRow();
        if (selectedRow == -1) {
            SwingUtils.showError(this, "Please select a recipe to view.");
            return;
        }
        
        int recipeId = (Integer) recipesTableModel.getValueAt(selectedRow, 0);
        Recipe recipe = recipeService.getRecipeById(recipeId);
        
        if (recipe != null) {
            RecipeDialog dialog = new RecipeDialog(this, "View Recipe", recipe, currentUser, 
                                                 recipeService, commentService, true);
            dialog.setVisible(true);
            refreshApprovedRecipes(); // Refresh in case rating/comment was added
        }
    }
    
    private void handleViewMyRecipe() {
        int selectedRow = myRecipesTable.getSelectedRow();
        if (selectedRow == -1) {
            SwingUtils.showError(this, "Please select a recipe to view.");
            return;
        }
        
        int recipeId = (Integer) myRecipesTableModel.getValueAt(selectedRow, 0);
        Recipe recipe = recipeService.getRecipeById(recipeId);
        
        if (recipe != null) {
            boolean readOnly = recipe.getStatus() != Recipe.RecipeStatus.PENDING;
            RecipeDialog dialog = new RecipeDialog(this, "View My Recipe", recipe, currentUser, 
                                                 recipeService, commentService, readOnly);
            dialog.setVisible(true);
            refreshMyRecipes();
        }
    }
    
    private void handleNewRecipe() {
        RecipeDialog dialog = new RecipeDialog(this, "New Recipe", null, currentUser, 
                                             recipeService, commentService, false);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            refreshMyRecipes();
            SwingUtils.showInfo(this, "Recipe created successfully! It will be reviewed by an administrator.");
        }
    }
    
    private void handleEditRecipe() {
        int selectedRow = myRecipesTable.getSelectedRow();
        if (selectedRow == -1) {
            SwingUtils.showError(this, "Please select a recipe to edit.");
            return;
        }
        
        int recipeId = (Integer) myRecipesTableModel.getValueAt(selectedRow, 0);
        Recipe recipe = recipeService.getRecipeById(recipeId);
        
        if (recipe != null) {
            if (recipe.getStatus() != Recipe.RecipeStatus.PENDING) {
                SwingUtils.showError(this, "You can only edit recipes that are still pending approval.");
                return;
            }
            
            RecipeDialog dialog = new RecipeDialog(this, "Edit Recipe", recipe, currentUser, 
                                                 recipeService, commentService, false);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                refreshMyRecipes();
            }
        }
    }
    
    private void handleDeleteRecipe() {
        int selectedRow = myRecipesTable.getSelectedRow();
        if (selectedRow == -1) {
            SwingUtils.showError(this, "Please select a recipe to delete.");
            return;
        }
        
        int recipeId = (Integer) myRecipesTableModel.getValueAt(selectedRow, 0);
        String title = (String) myRecipesTableModel.getValueAt(selectedRow, 1);
        
        if (SwingUtils.showConfirmation(this, "Are you sure you want to delete recipe '" + title + "'?")) {
            if (recipeService.deleteRecipe(recipeId)) {
                SwingUtils.showInfo(this, "Recipe deleted successfully.");
                refreshMyRecipes();
            } else {
                SwingUtils.showError(this, "Failed to delete recipe.");
            }
        }
    }
    
    private void handleEditProfile() {
        UserDialog dialog = new UserDialog(this, "Edit Profile", currentUser, userService);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            // Refresh the current user object
            currentUser = userService.getUserById(currentUser.getId());
            setTitle("Recipe Platform - User Dashboard (" + currentUser.getUsername() + ")");
            
            // Refresh profile tab by recreating it
            tabbedPane.removeTabAt(2);
            createProfileTab();
            
            SwingUtils.showInfo(this, "Profile updated successfully.");
        }
    }
    
    private void handleLogout() {
        if (SwingUtils.showConfirmation(this, "Are you sure you want to logout?")) {
            dispose();
            SwingUtils.runOnEDT(() -> new LoginFrame().setVisible(true));
        }
    }
    
    private void handleExit() {
        if (SwingUtils.showConfirmation(this, "Are you sure you want to exit?")) {
            System.exit(0);
        }
    }
    
    // ========== DATA CHANGE LISTENER METHODS ==========
    
    @Override
    public void onRecipesChanged() {
        System.out.println("UserFrame: Recipes changed, refreshing tables");
        SwingUtilities.invokeLater(() -> {
            refreshApprovedRecipes();
            refreshMyRecipes();
        });
    }
    
    @Override
    public void onUsersChanged() {
        System.out.println("UserFrame: Users changed");
        // User changes might affect recipe authors, so refresh recipes too
        SwingUtilities.invokeLater(() -> {
            refreshApprovedRecipes();
            refreshMyRecipes();
        });
    }
    
    @Override
    public void onCommentsChanged() {
        System.out.println("UserFrame: Comments changed, refreshing recipe data");
        SwingUtilities.invokeLater(() -> {
            refreshApprovedRecipes();
            refreshMyRecipes();
        });
    }
}
