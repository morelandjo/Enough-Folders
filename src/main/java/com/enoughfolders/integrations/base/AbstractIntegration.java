package com.enoughfolders.integrations.base;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.ModIntegration;
import com.enoughfolders.integrations.api.RecipeViewingIntegration;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Abstract base class for all mod integrations.
 */
public abstract class AbstractIntegration implements ModIntegration, RecipeViewingIntegration {
    
    /**
     * Flag to track if this integration has been initialized
     */
    protected boolean initialized = false;
    
    /**
     * Flag to track if this integration is available
     */
    protected boolean available = false;
    
    /**
     * The mod ID for this integration
     */
    protected final String modId;
    
    /**
     * The display name for this integration
     */
    protected final String displayName;
    
    /**
     * Error handler for integration operations
     */
    protected java.util.function.Consumer<Exception> errorHandler;
    
    /**
     * Creates a new abstract integration.
     *
     * @param modId The mod ID to check for availability
     * @param displayName The display name for this integration
     */
    protected AbstractIntegration(String modId, String displayName) {
        this.modId = modId;
        this.displayName = displayName;
        // Default error handler just logs the error
        this.errorHandler = (Exception e) -> logError("Integration error: {}", e.getMessage(), e);
    }
    
    /**
     * Initialize the integration.
     */
    @Override
    public void initialize() {
        if (initialized) {
            logDebug("Integration already initialized, skipping");
            return;
        }
        
        try {
            logInfo("Initializing {} integration", displayName);
            
            // Check if required classes are available
            if (checkClassAvailability()) {
                available = true;
                
                // Perform integration-specific initialization
                doInitialize();
                
                logInfo("{} integration initialized successfully", displayName);
            } else {
                available = false;
                logDebug("{} classes not found, integration disabled", displayName);
            }
            
            initialized = true;
            
        } catch (Exception e) {
            available = false;
            initialized = true;
            logError("Failed to initialize {} integration", displayName, e);
        }
    }
    
    /**
     * Checks if the required classes for this integration are available.
     *
     * @return true if all required classes are available, false otherwise
     */
    protected abstract boolean checkClassAvailability();
    
    /**
     * Performs the actual initialization logic specific to each integration.
     */
    protected abstract void doInitialize();
    
    @Override
    public String getModName() {
        return displayName;
    }
    
    @Override
    public boolean isAvailable() {
        return available;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the mod ID for this integration.
     *
     * @return The mod ID
     */
    public String getModId() {
        return modId;
    }
    
    /**
     * Checks if this integration has been initialized.
     *
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Safely executes an operation that might throw an exception.
     *
     * @param operation The operation to execute
     * @param operationName The name of the operation for logging
     * @param <T> The return type
     * @return Optional containing the result, or empty if an exception occurred
     */
    protected <T> Optional<T> safeExecute(SafeOperation<T> operation, String operationName) {
        try {
            T result = operation.execute();
            return Optional.ofNullable(result);
        } catch (Exception e) {
            logError("Error during {}: {}", operationName, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Logs an info message with integration context.
     *
     * @param message The message format
     * @param args The message arguments
     */
    protected void logInfo(String message, Object... args) {
        String formattedMessage = String.format("[%s] %s", displayName, message);
        EnoughFolders.LOGGER.info(formattedMessage, args);
    }
    
    /**
     * Logs a debug message with integration context.
     *
     * @param message The message format
     * @param args The message arguments
     */
    protected void logDebug(String message, Object... args) {
        String formattedMessage = String.format("[%s] %s", displayName, message);
        EnoughFolders.LOGGER.debug(formattedMessage, args);
        
        // Also log to DebugLogger if available
        DebugLogger.debugValue(
            DebugLogger.Category.INTEGRATION,
            formattedMessage, 
            args.length > 0 ? args[0] : ""
        );
    }
    
    /**
     * Logs an error message with integration context.
     *
     * @param message The message format
     * @param args The message arguments (last argument should be exception if present)
     */
    protected void logError(String message, Object... args) {
        String formattedMessage = String.format("[%s] %s", displayName, message);
        
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
     * Checks if a class exists by name.
     *
     * @param className The fully qualified class name to check
     * @return true if the class exists, false otherwise
     */
    protected boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Functional interface for safe operations.
     *
     * @param <T> The return type of the operation
     */
    @FunctionalInterface
    protected interface SafeOperation<T> {
        /**
         * Executes the operation and returns the result.
         *
         * @return The result of the operation
         * @throws Exception If the operation fails
         */
        T execute() throws Exception;
    }
    
    // Default implementations for RecipeViewingIntegration methods
    
    @Override
    public void showRecipes(Object ingredient) {
        logDebug("showRecipes called but not implemented for {}", displayName);
    }
    
    @Override
    public void showUses(Object ingredient) {
        logDebug("showUses called but not implemented for {}", displayName);
    }
    
    @Override
    public void connectToFolderScreen(FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen) {
        logDebug("connectToFolderScreen called but not implemented for {}", displayName);
    }
    
    @Override
    public void saveLastFolderScreen(FolderScreen folderScreen) {
        logDebug("saveLastFolderScreen called but not implemented for {}", displayName);
    }
    
    @Override
    public Optional<FolderScreen> getLastFolderScreen() {
        logDebug("getLastFolderScreen called but not implemented for {}", displayName);
        return Optional.empty();
    }
    
    @Override
    public void clearLastFolderScreen() {
        logDebug("clearLastFolderScreen called but not implemented for {}", displayName);
    }
    
    @Override
    public boolean isTransitioningToRecipeScreen(Screen screen) {
        logDebug("isTransitioningToRecipeScreen called but not implemented for {}", displayName);
        return false;
    }
    
    @Override
    public boolean isRecipeScreen(Screen screen) {
        logDebug("isRecipeScreen called but not implemented for {}", displayName);
        return false;
    }
    
    // Default implementations for ModIntegration methods
    
    @Override
    public Optional<?> getIngredientFromStored(StoredIngredient storedIngredient) {
        logDebug("getIngredientFromStored called but not implemented for {}", displayName);
        return Optional.empty();
    }
    
    @Override
    public Optional<StoredIngredient> storeIngredient(Object ingredient) {
        logDebug("storeIngredient called but not implemented for {}", displayName);
        return Optional.empty();
    }
    
    @Override
    public Optional<ItemStack> getItemStackForDisplay(Object ingredient) {
        logDebug("getItemStackForDisplay called but not implemented for {}", displayName);
        return Optional.empty();
    }
    
    @Override
    public void renderIngredient(GuiGraphics graphics, StoredIngredient ingredient, int x, int y, int width, int height) {
        logDebug("renderIngredient called but not implemented for {}", displayName);
    }
    
    /**
     * Gets the ingredient object currently under the mouse cursor.
     * 
     * @param screen The current screen
     * @param mouseX The mouse X coordinate
     * @param mouseY The mouse Y coordinate
     * @return Optional containing the ingredient under mouse, or empty if none
     */
    public Optional<?> getIngredientUnderMouse(Screen screen, double mouseX, double mouseY) {
        logDebug("getIngredientUnderMouse called but not implemented for {}", displayName);
        return Optional.empty();
    }
    
    /**
     * Sets a custom error handler for this integration.
     * 
     * @param errorHandler The error handler to use
     */
    public void setErrorHandler(java.util.function.Consumer<Exception> errorHandler) {
        this.errorHandler = errorHandler != null ? errorHandler : 
            (Exception e) -> logError("Integration error: {}", e.getMessage(), e);
    }
    
    /**
     * Gets the current error handler for this integration.
     * 
     * @return The current error handler
     */
    public java.util.function.Consumer<Exception> getErrorHandler() {
        return errorHandler;
    }
}
