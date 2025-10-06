package ui;

import models.Recipe;
import models.User;
import services.RecipeService;
import services.UserService;
import services.SyncManager;
import services.DataChangeListener;
import utils.SwingUtils;
import dao.InMemoryDB;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

/**
 * Admin dashboard providing user management, recipe approval, and statistics.
 * Allows admins to manage users, approve/reject recipes, and view platform statistics.
 */
public class AdminFrame extends JFrame implements DataChangeListener {
    private User currentUser;
    private UserService userService;
    private RecipeService recipeService;
    private SyncManager syncManager;
    
    // UI Components
    private JTabbedPane tabbedPane;
    private JTable usersTable;
    private JTable recipesTable;
    private DefaultTableModel usersTableModel;
    private DefaultTableModel recipesTableModel;
    private JPanel statsPanel;
    
    public AdminFrame(User user) {
        this.currentUser = user;
        this.userService = new UserService();
        this.recipeService = new RecipeService();
        this.syncManager = SyncManager.getInstance();
        
        // Register for data change notifications
        syncManager.addDataChangeListener(this);
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        refreshData();
        
        setTitle("Recipe Platform - Admin Dashboard (" + user.getUsername() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        SwingUtils.centerWindow(this);
    }
    
    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        tabbedPane = new JTabbedPane();
        
        // Users table
        String[] userColumns = {"ID", "Username", "Email", "Role"};
        usersTableModel = new DefaultTableModel(userColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        usersTable = new JTable(usersTableModel);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Recipes table
        String[] recipeColumns = {"ID", "Title", "Author", "Status", "Rating", "Comments"};
        recipesTableModel = new DefaultTableModel(recipeColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        recipesTable = new JTable(recipesTableModel);
        recipesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Statistics panel
        statsPanel = SwingUtils.createTitledPanel("Platform Statistics", new GridBagLayout());
    }
    
    /**
     * Setup the layout of components
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Create tabs
        createUsersTab();
        createRecipesTab();
        createStatisticsTab();
        
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
        
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }
    
    /**
     * Create users management tab
     */
    private void createUsersTab() {
        JPanel usersPanel = new JPanel(new BorderLayout());
        
        // Users table with scroll pane
        JScrollPane usersScrollPane = SwingUtils.createScrollPane(usersTable);
        usersPanel.add(usersScrollPane, BorderLayout.CENTER);
        
        // Users buttons panel
        JPanel usersButtonPanel = new JPanel(new FlowLayout());
        JButton addUserButton = SwingUtils.createButton("Add User", this::handleAddUser);
        JButton editUserButton = SwingUtils.createButton("Edit User", this::handleEditUser);
        JButton deleteUserButton = SwingUtils.createButton("Delete User", this::handleDeleteUser);
        JButton refreshUsersButton = SwingUtils.createButton("Refresh", this::refreshUsers);
        
        usersButtonPanel.add(addUserButton);
        usersButtonPanel.add(editUserButton);
        usersButtonPanel.add(deleteUserButton);
        usersButtonPanel.add(refreshUsersButton);
        
        usersPanel.add(usersButtonPanel, BorderLayout.SOUTH);
        
        tabbedPane.addTab("Users", usersPanel);
    }
    
    /**
     * Create recipes management tab
     */
    private void createRecipesTab() {
        JPanel recipesPanel = new JPanel(new BorderLayout());
        
        // Recipes table with scroll pane
        JScrollPane recipesScrollPane = SwingUtils.createScrollPane(recipesTable);
        recipesPanel.add(recipesScrollPane, BorderLayout.CENTER);
        
        // Recipes buttons panel
        JPanel recipesButtonPanel = new JPanel(new FlowLayout());
        JButton approveButton = SwingUtils.createButton("Approve Recipe", this::handleApproveRecipe);
        JButton rejectButton = SwingUtils.createButton("Reject Recipe", this::handleRejectRecipe);
        JButton viewRecipeButton = SwingUtils.createButton("View Recipe", this::handleViewRecipe);
        JButton deleteRecipeButton = SwingUtils.createButton("Delete Recipe", this::handleDeleteRecipe);
        JButton refreshRecipesButton = SwingUtils.createButton("Refresh", this::refreshRecipes);
        
        recipesButtonPanel.add(approveButton);
        recipesButtonPanel.add(rejectButton);
        recipesButtonPanel.add(viewRecipeButton);
        recipesButtonPanel.add(deleteRecipeButton);
        recipesButtonPanel.add(refreshRecipesButton);
        
        recipesPanel.add(recipesButtonPanel, BorderLayout.SOUTH);
        
        tabbedPane.addTab("Recipes", recipesPanel);
    }
    
    /**
     * Create statistics tab
     */
    private void createStatisticsTab() {
        JPanel mainStatsPanel = new JPanel(new BorderLayout());
        mainStatsPanel.add(statsPanel, BorderLayout.NORTH);
        
        JButton refreshStatsButton = SwingUtils.createButton("Refresh Statistics", this::refreshStatistics);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(refreshStatsButton);
        mainStatsPanel.add(buttonPanel, BorderLayout.CENTER);
        
        tabbedPane.addTab("Statistics", mainStatsPanel);
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        // Double-click to edit user
        usersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleEditUser();
                }
            }
        });
        
        // Double-click to view recipe
        recipesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleViewRecipe();
                }
            }
        });
    }
    
    /**
     * Refresh all data
     */
    private void refreshData() {
        refreshUsers();
        refreshRecipes();
        refreshStatistics();
    }
    
    /**
     * Refresh users table
     */
    private void refreshUsers() {
        usersTableModel.setRowCount(0);
        List<User> users = userService.getAllUsers();
        
        for (User user : users) {
            Object[] row = {
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
            };
            usersTableModel.addRow(row);
        }
    }
    
    /**
     * Refresh recipes table
     */
    private void refreshRecipes() {
        recipesTableModel.setRowCount(0);
        List<Recipe> recipes = recipeService.getAllRecipes();
        
        for (Recipe recipe : recipes) {
            Object[] row = {
                recipe.getId(),
                recipe.getTitle(),
                recipe.getAuthorName(),
                recipe.getStatus(),
                SwingUtils.formatRating(recipe.getAverageRating()),
                recipe.getComments().size()
            };
            recipesTableModel.addRow(row);
        }
    }
    
    /**
     * Refresh statistics
     */
    private void refreshStatistics() {
        statsPanel.removeAll();
        
        Map<String, Integer> stats = InMemoryDB.getInstance().getStatistics();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            String label = formatStatLabel(entry.getKey());
            
            gbc.gridx = 0; gbc.gridy = row;
            statsPanel.add(SwingUtils.createLabel(label + ":", JLabel.LEFT), gbc);
            
            gbc.gridx = 1; gbc.gridy = row;
            statsPanel.add(SwingUtils.createLabel(entry.getValue().toString(), JLabel.LEFT), gbc);
            
            row++;
        }
        
        statsPanel.revalidate();
        statsPanel.repaint();
    }
    
    /**
     * Format statistic label for display
     */
    private String formatStatLabel(String key) {
        switch (key) {
            case "totalUsers": return "Total Users";
            case "totalRecipes": return "Total Recipes";
            case "approvedRecipes": return "Approved Recipes";
            case "pendingRecipes": return "Pending Recipes";
            case "rejectedRecipes": return "Rejected Recipes";
            case "totalComments": return "Total Comments";
            default: return key;
        }
    }
    
    // ========== EVENT HANDLERS ==========
    
    private void handleAddUser() {
        UserDialog dialog = new UserDialog(this, "Add User", null, userService);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            refreshUsers();
        }
    }
    
    private void handleEditUser() {
        int selectedRow = usersTable.getSelectedRow();
        if (selectedRow == -1) {
            SwingUtils.showError(this, "Please select a user to edit.");
            return;
        }
        
        int userId = (Integer) usersTableModel.getValueAt(selectedRow, 0);
        User user = userService.getUserById(userId);
        
        if (user != null) {
            UserDialog dialog = new UserDialog(this, "Edit User", user, userService);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                refreshUsers();
            }
        }
    }
    
    private void handleDeleteUser() {
        int selectedRow = usersTable.getSelectedRow();
        if (selectedRow == -1) {
            SwingUtils.showError(this, "Please select a user to delete.");
            return;
        }
        
        int userId = (Integer) usersTableModel.getValueAt(selectedRow, 0);
        String username = (String) usersTableModel.getValueAt(selectedRow, 1);
        
        if (userId == currentUser.getId()) {
            SwingUtils.showError(this, "You cannot delete your own account.");
            return;
        }
        
        if (SwingUtils.showConfirmation(this, "Are you sure you want to delete user '" + username + "'?")) {
            if (userService.deleteUser(userId)) {
                SwingUtils.showInfo(this, "User deleted successfully.");
                refreshUsers();
            } else {
                SwingUtils.showError(this, "Failed to delete user.");
            }
        }
    }
    
    private void handleApproveRecipe() {
        int selectedRow = recipesTable.getSelectedRow();
        if (selectedRow == -1) {
            SwingUtils.showError(this, "Please select a recipe to approve.");
            return;
        }
        
        int recipeId = (Integer) recipesTableModel.getValueAt(selectedRow, 0);
        
        if (recipeService.approveRecipe(recipeId)) {
            SwingUtils.showInfo(this, "Recipe approved successfully.");
            refreshRecipes();
        } else {
            SwingUtils.showError(this, "Failed to approve recipe.");
        }
    }
    
    private void handleRejectRecipe() {
        int selectedRow = recipesTable.getSelectedRow();
        if (selectedRow == -1) {
            SwingUtils.showError(this, "Please select a recipe to reject.");
            return;
        }
        
        int recipeId = (Integer) recipesTableModel.getValueAt(selectedRow, 0);
        String title = (String) recipesTableModel.getValueAt(selectedRow, 1);
        
        if (SwingUtils.showConfirmation(this, "Are you sure you want to reject recipe '" + title + "'?")) {
            if (recipeService.rejectRecipe(recipeId)) {
                SwingUtils.showInfo(this, "Recipe rejected.");
                refreshRecipes();
            } else {
                SwingUtils.showError(this, "Failed to reject recipe.");
            }
        }
    }
    
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
                                                 recipeService, null, true);
            dialog.setVisible(true);
            refreshRecipes();
        }
    }
    
    private void handleDeleteRecipe() {
        int selectedRow = recipesTable.getSelectedRow();
        if (selectedRow == -1) {
            SwingUtils.showError(this, "Please select a recipe to delete.");
            return;
        }
        
        int recipeId = (Integer) recipesTableModel.getValueAt(selectedRow, 0);
        String title = (String) recipesTableModel.getValueAt(selectedRow, 1);
        
        if (SwingUtils.showConfirmation(this, "Are you sure you want to delete recipe '" + title + "'?")) {
            if (recipeService.deleteRecipe(recipeId)) {
                SwingUtils.showInfo(this, "Recipe deleted successfully.");
                refreshRecipes();
            } else {
                SwingUtils.showError(this, "Failed to delete recipe.");
            }
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
        System.out.println("AdminFrame: Recipes changed, refreshing table");
        SwingUtilities.invokeLater(this::refreshRecipes);
    }
    
    @Override
    public void onUsersChanged() {
        System.out.println("AdminFrame: Users changed, refreshing table");
        SwingUtilities.invokeLater(this::refreshUsers);
    }
    
    @Override
    public void onCommentsChanged() {
        System.out.println("AdminFrame: Comments changed, refreshing data");
        SwingUtilities.invokeLater(this::refreshStatistics);
    }
}
