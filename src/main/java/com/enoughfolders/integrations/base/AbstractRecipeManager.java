package com.enoughfolders.integrations.base;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.Optional;

/**
 * Abstract base class for all recipe managers.
 * Provides common functionality for recipe viewing, GUI interaction,
 * and navigation that is shared across all integrations.
 */
public abstract class AbstractRecipeManager {
    
    /**
     * The integration name for this recipe manager
     */
    protected final String integrationName;
    
    /**
     * Saved folder screen for navigation back from recipe GUI
     */
    protected FolderScreen savedFolderScreen;
    
    /**
     * The last container screen associated with the folder
     */
    protected AbstractContainerScreen<?> lastContainerScreen;
    
    /**
     * Creates a new abstract recipe manager.
     *
     * @param integrationName The name of the integration this manager belongs to
     */
    protected AbstractRecipeManager(String integrationName) {
        this.integrationName = integrationName;
    }
    
    /**
     * Shows recipes for the provided ingredient in the recipe GUI.
     * This method provides common error handling and delegates to the specific implementation.
     *
     * @param ingredient The ingredient to show recipes for
     */
    public void showRecipes(Object ingredient) {
        if (ingredient == null) {
            logDebug("Ingredient is null, cannot show recipes");
            return;
        }
        
        safeExecute(
            () -> doShowRecipes(ingredient),
            "showing recipes"
        );
    }
    
    /**
     * Performs the actual recipe showing logic.
     * Subclasses must implement this with their specific recipe viewing logic.
     *
     * @param ingredient The ingredient to show recipes for
     */
    protected abstract void doShowRecipes(Object ingredient);
    
    /**
     * Shows usages for the provided ingredient in the recipe GUI.
     * This method provides common error handling and delegates to the specific implementation.
     *
     * @param ingredient The ingredient to show uses for
     */
    public void showUses(Object ingredient) {
        if (ingredient == null) {
            logDebug("Ingredient is null, cannot show uses");
            return;
        }
        
        safeExecute(
            () -> doShowUses(ingredient),
            "showing uses"
        );
    }
    
    /**
     * Performs the actual usage showing logic.
     * Subclasses must implement this with their specific usage viewing logic.
     *
     * @param ingredient The ingredient to show uses for
     */
    protected abstract void doShowUses(Object ingredient);
    
    /**
     * Connects a folder screen to the recipe viewer for ingredient interactions.
     * This method provides common setup and delegates to the specific implementation.
     *
     * @param folderScreen The folder screen to connect
     * @param containerScreen The container screen the folder is attached to
     */
    public void connectToFolderScreen(FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen) {
        if (folderScreen == null) {
            logDebug("FolderScreen is null, cannot connect");
            return;
        }
        
        this.savedFolderScreen = folderScreen;
        this.lastContainerScreen = containerScreen;
        
        logDebug("Connected to folder screen: {}", folderScreen.getClass().getSimpleName());
        
        safeExecute(
            () -> doConnectToFolderScreen(folderScreen, containerScreen),
            "connecting to folder screen"
        );
    }
    
    /**
     * Performs the actual folder screen connection logic.
     * Subclasses can override this to add specific connection logic.
     * Default implementation does nothing.
     *
     * @param folderScreen The folder screen to connect
     * @param containerScreen The container screen the folder is attached to
     */
    protected void doConnectToFolderScreen(FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen) {
        // Default implementation - subclasses can override
        logDebug("Default folder screen connection (no specific logic)");
    }
    
    /**
     * Saves a folder screen to be used during recipe GUI navigation.
     *
     * @param folderScreen The folder screen to save
     */
    public void saveFolderScreen(FolderScreen folderScreen) {
        this.savedFolderScreen = folderScreen;
        logDebug("Saved folder screen for navigation");
    }
    
    /**
     * Gets the saved folder screen.
     *
     * @return Optional containing the saved folder screen, or empty if none saved
     */
    public Optional<FolderScreen> getSavedFolderScreen() {
        return Optional.ofNullable(savedFolderScreen);
    }
    
    /**
     * Clears the saved folder screen.
     */
    public void clearSavedFolderScreen() {
        this.savedFolderScreen = null;
        this.lastContainerScreen = null;
        logDebug("Cleared saved folder screen");
    }
    
    /**
     * Checks if we can navigate back to the folder screen.
     *
     * @return true if navigation back is possible, false otherwise
     */
    public boolean canNavigateBack() {
        return savedFolderScreen != null;
    }
    
    /**
     * Navigates back to the saved folder screen.
     * This method provides common navigation logic and delegates to the specific implementation.
     */
    public void navigateBack() {
        if (!canNavigateBack()) {
            logDebug("Cannot navigate back: no saved folder screen");
            return;
        }
        
        safeExecute(
            () -> doNavigateBack(),
            "navigating back to folder screen"
        );
    }
    
    /**
     * Performs the actual navigation back logic.
     * Subclasses should implement this with their specific navigation logic.
     * Default implementation does nothing.
     */
    protected void doNavigateBack() {
        // Default implementation - subclasses should override
        logDebug("Default navigation back (no specific logic)");
    }
    
    /**
     * Checks if the recipe GUI is currently open.
     * Subclasses should override this to provide specific detection logic.
     *
     * @return true if recipe GUI is open, false otherwise
     */
    public boolean isRecipeGuiOpen() {
        return false; // Default implementation
    }
    
    /**
     * Gets the current screen if it's a recipe GUI screen.
     * Subclasses should override this to provide specific screen detection.
     *
     * @return Optional containing the recipe screen, or empty if not a recipe screen
     */
    public Optional<Screen> getCurrentRecipeScreen() {
        return Optional.empty(); // Default implementation
    }
    
    /**
     * Handles when the recipe GUI is opened.
     * This can be overridden by subclasses to add specific logic.
     *
     * @param recipeScreen The recipe screen that was opened
     */
    protected void onRecipeGuiOpened(Screen recipeScreen) {
        logDebug("Recipe GUI opened: {}", recipeScreen.getClass().getSimpleName());
    }
    
    /**
     * Handles when the recipe GUI is closed.
     * This can be overridden by subclasses to add specific logic.
     */
    protected void onRecipeGuiClosed() {
        logDebug("Recipe GUI closed");
    }
    
    /**
     * Safely executes an operation that might throw an exception.
     * Logs any exceptions that occur.
     *
     * @param operation The operation to execute
     * @param operationName The name of the operation for logging
     */
    protected void safeExecute(SafeOperation operation, String operationName) {
        try {
            operation.execute();
        } catch (Exception e) {
            logError("Error during {}: {}", operationName, e.getMessage(), e);
        }
    }
    
    /**
     * Logs an info message with recipe manager context.
     *
     * @param message The message format
     * @param args The message arguments
     */
    protected void logInfo(String message, Object... args) {
        String formattedMessage = String.format("[%s RecipeManager] %s", integrationName, message);
        EnoughFolders.LOGGER.info(formattedMessage, args);
    }
    
    /**
     * Logs a debug message with recipe manager context.
     *
     * @param message The message format
     * @param args The message arguments
     */
    protected void logDebug(String message, Object... args) {
        String formattedMessage = String.format("[%s RecipeManager] %s", integrationName, message);
        EnoughFolders.LOGGER.debug(formattedMessage, args);
        
        // Also log to DebugLogger if available
        DebugLogger.debugValue(
            DebugLogger.Category.INTEGRATION,
            formattedMessage, 
            args.length > 0 ? args[0] : ""
        );
    }
    
    /**
     * Logs an error message with recipe manager context.
     *
     * @param message The message format
     * @param args The message arguments (last argument should be exception if present)
     */
    protected void logError(String message, Object... args) {
        String formattedMessage = String.format("[%s RecipeManager] %s", integrationName, message);
        
        // Check if last argument is an exception
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            Object[] messageArgs = new Object[args.length - 1];
            System.arraycopy(args, 0, messageArgs, 0, args.length - 1);
            Throwable exception = (Throwable) args[args.length - 1];
            EnoughFolders.LOGGER.error(formattedMessage, messageArgs, exception);
        } else {
            EnoughFolders.LOGGER.error(formattedMessage, args);
        }
    }
    
    /**
     * Functional interface for safe operations.
     */
    @FunctionalInterface
    protected interface SafeOperation {
        /**
         * Executes the operation.
         *
         * @throws Exception If the operation fails
         */
        void execute() throws Exception;
    }
}
