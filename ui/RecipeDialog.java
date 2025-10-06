package ui;

import models.Comment;
import models.Recipe;
import models.User;
import services.CommentService;
import services.RecipeService;
import utils.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Dialog for creating, editing, and viewing recipes.
 * Supports different modes: create new, edit existing, and read-only view.
 * Also allows rating and commenting on approved recipes.
 */
public class RecipeDialog extends JDialog {
    private Recipe recipe;
    private User currentUser;
    private RecipeService recipeService;
    private CommentService commentService;
    private boolean readOnly;
    private boolean confirmed = false;
    
    // UI Components
    private JTextField titleField;
    private JTextArea ingredientsArea;
    private JTextArea instructionsArea;
    private JPanel commentsPanel;
    private JTextArea newCommentArea;
    private JComboBox<Integer> ratingComboBox;
    private JButton saveButton;
    private JButton cancelButton;
    private JButton addCommentButton;
    private JButton addRatingButton;
    
    public RecipeDialog(Window parent, String title, Recipe recipe, User currentUser,
                       RecipeService recipeService, CommentService commentService, boolean readOnly) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        
        this.recipe = recipe;
        this.currentUser = currentUser;
        this.recipeService = recipeService;
        this.commentService = commentService;
        this.readOnly = readOnly;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        populateFields();
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
    }
    
    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        titleField = SwingUtils.createTextField(30);
        titleField.setEditable(!readOnly);
        
        ingredientsArea = SwingUtils.createTextArea(8, 40);
        ingredientsArea.setEditable(!readOnly);
        
        instructionsArea = SwingUtils.createTextArea(8, 40);
        instructionsArea.setEditable(!readOnly);
        
        commentsPanel = new JPanel();
        commentsPanel.setLayout(new BoxLayout(commentsPanel, BoxLayout.Y_AXIS));
        
        newCommentArea = SwingUtils.createTextArea(3, 40);
        
        // Rating combo box (1-5 stars)
        Integer[] ratings = {1, 2, 3, 4, 5};
        ratingComboBox = new JComboBox<>(ratings);
        
        // Buttons
        if (readOnly) {
            saveButton = null;
            cancelButton = SwingUtils.createButton("Close", this::handleCancel);
        } else {
            saveButton = SwingUtils.createButton("Save", this::handleSave);
            cancelButton = SwingUtils.createButton("Cancel", this::handleCancel);
        }
        
        addCommentButton = SwingUtils.createButton("Add Comment", this::handleAddComment);
        addRatingButton = SwingUtils.createButton("Add Rating", this::handleAddRating);
        
        // Enable comment/rating buttons only for approved recipes and non-authors
        boolean canInteract = recipe != null && 
                             recipe.getStatus() == Recipe.RecipeStatus.APPROVED &&
                             recipe.getAuthorId() != currentUser.getId() &&
                             commentService != null;
        
        addCommentButton.setEnabled(canInteract);
        addRatingButton.setEnabled(canInteract);
        newCommentArea.setEditable(canInteract);
    }
    
    /**
     * Setup the layout of components
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Main panel with tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Recipe details tab
        JPanel recipePanel = createRecipePanel();
        tabbedPane.addTab("Recipe Details", recipePanel);
        
        // Comments and ratings tab (only for existing recipes)
        if (recipe != null) {
            JPanel interactionPanel = createInteractionPanel();
            tabbedPane.addTab("Comments & Ratings", interactionPanel);
        }
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        if (saveButton != null) {
            buttonPanel.add(saveButton);
        }
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Create recipe details panel
     */
    private JPanel createRecipePanel() {
        JPanel panel = SwingUtils.createPaddedPanel(new GridBagLayout(), 10);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.NORTHEAST;
        panel.add(SwingUtils.createLabel("Title:", JLabel.RIGHT), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.NORTHWEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(titleField, gbc);
        
        // Ingredients
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.NORTHEAST; gbc.fill = GridBagConstraints.NONE;
        panel.add(SwingUtils.createLabel("Ingredients:", JLabel.RIGHT), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.NORTHWEST; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 0.3;
        panel.add(SwingUtils.createScrollPane(ingredientsArea), gbc);
        
        // Instructions
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.NORTHEAST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.weighty = 0;
        panel.add(SwingUtils.createLabel("Instructions:", JLabel.RIGHT), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.NORTHWEST; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 0.7;
        panel.add(SwingUtils.createScrollPane(instructionsArea), gbc);
        
        // Recipe info (for existing recipes)
        if (recipe != null) {
            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weighty = 0;
            JPanel infoPanel = createRecipeInfoPanel();
            panel.add(infoPanel, gbc);
        }
        
        return panel;
    }
    
    /**
     * Create recipe info panel (status, author, rating)
     */
    private JPanel createRecipeInfoPanel() {
        JPanel panel = SwingUtils.createTitledPanel("Recipe Information", new GridLayout(0, 2, 5, 5));
        
        panel.add(SwingUtils.createLabel("Author:", JLabel.LEFT));
        panel.add(SwingUtils.createLabel(recipe.getAuthorName(), JLabel.LEFT));
        
        panel.add(SwingUtils.createLabel("Status:", JLabel.LEFT));
        panel.add(SwingUtils.createLabel(recipe.getStatus().toString(), JLabel.LEFT));
        
        panel.add(SwingUtils.createLabel("Average Rating:", JLabel.LEFT));
        panel.add(SwingUtils.createLabel(SwingUtils.formatRating(recipe.getAverageRating()), JLabel.LEFT));
        
        panel.add(SwingUtils.createLabel("Total Ratings:", JLabel.LEFT));
        panel.add(SwingUtils.createLabel(String.valueOf(recipe.getRatings().size()), JLabel.LEFT));
        
        return panel;
    }
    
    /**
     * Create comments and ratings interaction panel
     */
    private JPanel createInteractionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Comments section
        JPanel commentsSection = SwingUtils.createTitledPanel("Comments", new BorderLayout());
        refreshComments();
        JScrollPane commentsScrollPane = SwingUtils.createScrollPane(commentsPanel);
        commentsScrollPane.setPreferredSize(new Dimension(500, 200));
        commentsSection.add(commentsScrollPane, BorderLayout.CENTER);
        
        // Add comment section
        JPanel addCommentSection = SwingUtils.createTitledPanel("Add Comment", new BorderLayout());
        addCommentSection.add(SwingUtils.createScrollPane(newCommentArea), BorderLayout.CENTER);
        addCommentSection.add(addCommentButton, BorderLayout.SOUTH);
        
        // Rating section
        JPanel ratingSection = SwingUtils.createTitledPanel("Add Rating", new FlowLayout());
        ratingSection.add(SwingUtils.createLabel("Rating:", JLabel.LEFT));
        ratingSection.add(ratingComboBox);
        ratingSection.add(addRatingButton);
        
        // Combine interaction panels
        JPanel interactionPanel = new JPanel(new BorderLayout());
        interactionPanel.add(addCommentSection, BorderLayout.NORTH);
        interactionPanel.add(ratingSection, BorderLayout.CENTER);
        
        // Main layout
        panel.add(commentsSection, BorderLayout.CENTER);
        panel.add(interactionPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        // Set default button
        if (saveButton != null) {
            getRootPane().setDefaultButton(saveButton);
        } else {
            getRootPane().setDefaultButton(cancelButton);
        }
    }
    
    /**
     * Populate fields with recipe data
     */
    private void populateFields() {
        if (recipe != null) {
            titleField.setText(recipe.getTitle());
            ingredientsArea.setText(recipe.getIngredients());
            instructionsArea.setText(recipe.getInstructions());
        }
    }
    
    /**
     * Refresh comments display
     */
    private void refreshComments() {
        commentsPanel.removeAll();
        
        if (recipe != null && commentService != null) {
            List<Comment> comments = commentService.getCommentsByRecipe(recipe.getId());
            
            if (comments.isEmpty()) {
                commentsPanel.add(SwingUtils.createLabel("No comments yet.", JLabel.CENTER));
            } else {
                for (Comment comment : comments) {
                    JPanel commentPanel = createCommentPanel(comment);
                    commentsPanel.add(commentPanel);
                    commentsPanel.add(Box.createVerticalStrut(5));
                }
            }
        }
        
        commentsPanel.revalidate();
        commentsPanel.repaint();
    }
    
    /**
     * Create a panel for displaying a single comment
     */
    private JPanel createCommentPanel(Comment comment) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        // Author and date
        JLabel authorLabel = SwingUtils.createLabel(
            comment.getAuthorName() + " - " + comment.getCreatedAt().toLocalDate(),
            JLabel.LEFT
        );
        authorLabel.setFont(authorLabel.getFont().deriveFont(Font.BOLD));
        panel.add(authorLabel, BorderLayout.NORTH);
        
        // Comment text
        JTextArea textArea = new JTextArea(comment.getText());
        textArea.setEditable(false);
        textArea.setBackground(panel.getBackground());
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        panel.add(textArea, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ========== EVENT HANDLERS ==========
    
    private void handleSave() {
        try {
            String title = titleField.getText().trim();
            String ingredients = ingredientsArea.getText().trim();
            String instructions = instructionsArea.getText().trim();
            
            if (recipe == null) {
                // Creating new recipe
                recipeService.createRecipe(title, ingredients, instructions, currentUser);
            } else {
                // Updating existing recipe
                recipe.setTitle(title);
                recipe.setIngredients(ingredients);
                recipe.setInstructions(instructions);
                recipeService.updateRecipe(recipe);
            }
            
            confirmed = true;
            dispose();
            
        } catch (IllegalArgumentException e) {
            SwingUtils.showError(this, e.getMessage());
        } catch (Exception e) {
            SwingUtils.showError(this, "Failed to save recipe: " + e.getMessage());
        }
    }
    
    private void handleCancel() {
        dispose();
    }
    
    private void handleAddComment() {
        try {
            String commentText = newCommentArea.getText().trim();
            
            if (commentText.isEmpty()) {
                SwingUtils.showError(this, "Please enter a comment.");
                return;
            }
            
            commentService.addComment(commentText, currentUser, recipe.getId());
            newCommentArea.setText("");
            refreshComments();
            SwingUtils.showInfo(this, "Comment added successfully!");
            
        } catch (IllegalArgumentException e) {
            SwingUtils.showError(this, e.getMessage());
        } catch (Exception e) {
            SwingUtils.showError(this, "Failed to add comment: " + e.getMessage());
        }
    }
    
    private void handleAddRating() {
        try {
            int rating = (Integer) ratingComboBox.getSelectedItem();
            
            recipeService.addRating(recipe.getId(), rating);
            SwingUtils.showInfo(this, "Rating added successfully!");
            
            // Refresh recipe info if available
            recipe = recipeService.getRecipeById(recipe.getId());
            
        } catch (IllegalArgumentException e) {
            SwingUtils.showError(this, e.getMessage());
        } catch (Exception e) {
            SwingUtils.showError(this, "Failed to add rating: " + e.getMessage());
        }
    }
    
    /**
     * Check if the dialog was confirmed (saved)
     */
    public boolean isConfirmed() {
        return confirmed;
    }
}
