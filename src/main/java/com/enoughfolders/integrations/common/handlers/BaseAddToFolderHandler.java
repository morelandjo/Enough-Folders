package com.enoughfolders.integrations.common.handlers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Base handler for adding ingredients to folders through integration shortcuts.
 */
public abstract class BaseAddToFolderHandler {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    protected BaseAddToFolderHandler() {
        // Base class should not be instantiated directly
    }

    /**
     * Handles the key press to add or remove an ingredient from the active folder.
     *
     * @param integrationName The name of the integration for logging purposes
     * @param retriever The ingredient retriever for the specific integration
     */
    public static void handleAddToFolderKeyPress(String integrationName, IngredientRetriever retriever) {
        EnoughFolders.LOGGER.info("{} Add to Folder handler activated", integrationName);

        // Make sure we have an active folder
        FolderManager folderManager = EnoughFolders.getInstance().getFolderManager();
        Optional<Folder> activeFolder = folderManager.getActiveFolder();
        if (activeFolder.isEmpty()) {
            // No active folder, display a message to the user
            var player = Minecraft.getInstance().player;
            if (player != null) {
                EnoughFolders.LOGGER.debug("No active folder available, displaying message to player");
                        }
            DebugLogger.debug(DebugLogger.Category.INPUT, 
                "No active folder available for adding ingredient");
            return;
        }

        EnoughFolders.LOGGER.debug("Active folder found: {}", activeFolder.get().getName());

        // First, check if mouse is hovering over a folder ingredient slot for removal
        Optional<StoredIngredient> hoveredIngredient = getHoveredFolderIngredient();
        if (hoveredIngredient.isPresent()) {
            // Remove the ingredient from the folder
            folderManager.removeIngredient(activeFolder.get(), hoveredIngredient.get());
            DebugLogger.debugValues(DebugLogger.Category.INPUT, 
                "Removed {} ingredient from folder '{}'", integrationName, activeFolder.get().getName());
            
            var player = Minecraft.getInstance().player;
            if (player != null) {
                EnoughFolders.LOGGER.debug("Ingredient removed from folder, displaying message to player");
            }
            return;
        }

        try {
            // Try to get ingredient under cursor using integration-specific method
            Optional<Object> ingredientOpt = retriever.getIngredientUnderCursor();
            
            if (ingredientOpt.isPresent()) {
                Object ingredient = ingredientOpt.get();
                EnoughFolders.LOGGER.info("Found {} ingredient under cursor: {}", 
                    integrationName, ingredient.getClass().getSimpleName());

                // Convert ingredient to StoredIngredient using integration-specific method
                Optional<StoredIngredient> storedIngredientOpt = retriever.convertToStoredIngredient(ingredient);
                
                if (storedIngredientOpt.isPresent()) {
                    StoredIngredient storedIngredient = storedIngredientOpt.get();
                    EnoughFolders.LOGGER.debug("Successfully converted to StoredIngredient: {}", storedIngredient);

                    // Add the ingredient to the folder
                    folderManager.addIngredient(activeFolder.get(), storedIngredient);

                    DebugLogger.debugValues(DebugLogger.Category.INPUT, 
                        "Added {} ingredient to folder '{}'", integrationName, activeFolder.get().getName());
                } else {
                    EnoughFolders.LOGGER.error("Failed to convert {} ingredient to StoredIngredient", integrationName);
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        EnoughFolders.LOGGER.debug("Conversion failed, displaying message to player");
                    }
                }
            } else {
                EnoughFolders.LOGGER.debug("No {} ingredient found under cursor", integrationName);
                
                // Check if the integration overlay is visible
                var player = Minecraft.getInstance().player;
                if (!retriever.isOverlayVisible()) {
                    if (player != null) {
                        EnoughFolders.LOGGER.debug("Integration overlay not visible, cannot retrieve ingredient");
                    }
                } else {
                    if (player != null) {
                        EnoughFolders.LOGGER.debug("Integration overlay not visible, cannot retrieve ingredient");
                    }
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error interacting with {} runtime", integrationName, e);
            var player = Minecraft.getInstance().player;
            if (player != null) {
                EnoughFolders.LOGGER.debug("Exception occurred, displaying error message to player");
                player.displayClientMessage(Component.translatable("enoughfolders.error.runtime_interaction", integrationName), false);
            }
        }
    }

    /**
     * Interface for integration-specific ingredient retrieval operations.
     */
    public interface IngredientRetriever {
        /**
         * Get the ingredient currently under the mouse cursor.
         * @return Optional containing the ingredient if found, empty otherwise
         */
        Optional<Object> getIngredientUnderCursor();

        /**
         * Convert an integration-specific ingredient to a StoredIngredient.
         * @param ingredient The integration ingredient to convert
         * @return Optional containing the StoredIngredient if conversion successful, empty otherwise
         */
        Optional<StoredIngredient> convertToStoredIngredient(Object ingredient);

        /**
         * Check if the integration's overlay/interface is currently visible.
         * @return true if the overlay is visible and can provide ingredients, false otherwise
         */
        boolean isOverlayVisible();
    }

    /**
     * Validates that an integration is available before attempting operations.
     * @param integrationName The name of the integration for logging
     * @param isAvailable Supplier that checks if the integration is available
     * @return true if the integration is available, false otherwise
     */
    protected static boolean validateIntegrationAvailable(String integrationName, boolean isAvailable) {
        if (!isAvailable) {
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "{} integration not available", integrationName);
            return false;
        }
        return true;
    }

    /**
     * Checks if the required integration classes exist in the classpath.
     * @param classNames Array of class names to check
     * @return true if all classes exist, false otherwise
     */
    protected static boolean checkIntegrationClasses(String... classNames) {
        for (String className : classNames) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                    "Integration class not found: {}", className);
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the folder ingredient currently under the mouse cursor.
     * 
     * @return Optional containing the StoredIngredient if hovering over one, empty otherwise
     */
    private static Optional<StoredIngredient> getHoveredFolderIngredient() {
        try {
            // Get current mouse position scaled to GUI coordinates
            Minecraft minecraft = Minecraft.getInstance();
            double mouseX = minecraft.mouseHandler.xpos() * 
                (double)minecraft.getWindow().getGuiScaledWidth() / 
                (double)minecraft.getWindow().getScreenWidth();
            double mouseY = minecraft.mouseHandler.ypos() * 
                (double)minecraft.getWindow().getGuiScaledHeight() / 
                (double)minecraft.getWindow().getScreenHeight();
            
            DebugLogger.debugValues(DebugLogger.Category.INPUT, 
                "Mouse position: ({}, {})", (int)mouseX, (int)mouseY);
            
            // Get the current folder screen
            Optional<com.enoughfolders.client.gui.FolderScreen> folderScreenOpt = getCurrentFolderScreen();
            if (folderScreenOpt.isEmpty()) {
                DebugLogger.debug(DebugLogger.Category.INPUT, "No folder screen available for hover detection");
                return Optional.empty();
            }
            
            com.enoughfolders.client.gui.FolderScreen folderScreen = folderScreenOpt.get();
            DebugLogger.debug(DebugLogger.Category.INPUT, "Found folder screen for hover detection");
            
            // Check if mouse is within folder screen bounds
            boolean isVisible = folderScreen.isVisible(mouseX, mouseY);
            DebugLogger.debugValues(DebugLogger.Category.INPUT, 
                "Folder screen visibility check: {}", isVisible);
            
            if (!isVisible) {
                DebugLogger.debug(DebugLogger.Category.INPUT, "Mouse not over folder screen area");
                return Optional.empty();
            }
            
            // Check all ingredient slots for hover
            var ingredientSlots = folderScreen.getIngredientSlots();
            DebugLogger.debugValues(DebugLogger.Category.INPUT, 
                "Checking {} ingredient slots for hover", ingredientSlots.size());
            
            for (var slot : ingredientSlots) {
                boolean isHovered = slot.isHovered((int)mouseX, (int)mouseY);
                if (isHovered) {
                    DebugLogger.debugValues(DebugLogger.Category.INPUT, 
                        "Found hovered ingredient slot at mouse position ({}, {})", (int)mouseX, (int)mouseY);
                    StoredIngredient ingredient = slot.getIngredient();
                    DebugLogger.debugValues(DebugLogger.Category.INPUT, 
                        "Ingredient in hovered slot: {}", ingredient != null ? ingredient.toString() : "null");
                    return Optional.of(ingredient);
                }
            }
            
            DebugLogger.debug(DebugLogger.Category.INPUT, "No ingredient slot hovered at current mouse position");
            return Optional.empty();
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error detecting hovered folder ingredient", e);
            return Optional.empty();
        }
    }

    /**
     * Gets the current folder screen from either container screens or recipe screens.
     * 
     * @return Optional containing the FolderScreen if available, empty otherwise
     */
    private static Optional<com.enoughfolders.client.gui.FolderScreen> getCurrentFolderScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        DebugLogger.debugValues(DebugLogger.Category.INPUT, 
            "Current screen: {}", minecraft.screen != null ? minecraft.screen.getClass().getSimpleName() : "null");
        
        // First try container screens
        if (minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen) {
            DebugLogger.debug(DebugLogger.Category.INPUT, "Checking container screen for folder screen");
            var folderScreenOpt = com.enoughfolders.client.event.ClientEventHandler.getFolderScreen(containerScreen);
            if (folderScreenOpt.isPresent()) {
                DebugLogger.debug(DebugLogger.Category.INPUT, "Found folder screen from container screen");
                return folderScreenOpt;
            } else {
                DebugLogger.debug(DebugLogger.Category.INPUT, "No folder screen found from container screen");
            }
        }
        
        // Then try recipe screens through integrations
        try {
            DebugLogger.debug(DebugLogger.Category.INPUT, "Checking recipe screen integrations for folder screen");
            
            // Check JEI integration
            var jeiIntegrationOpt = com.enoughfolders.di.DependencyProvider.get(
                com.enoughfolders.integrations.jei.core.JEIIntegration.class);
            if (jeiIntegrationOpt.isPresent()) {
                var jeiIntegration = jeiIntegrationOpt.get();
                if (jeiIntegration.isAvailable()) {
                    var folderScreenOpt = jeiIntegration.getLastFolderScreen();
                    if (folderScreenOpt.isPresent()) {
                        DebugLogger.debug(DebugLogger.Category.INPUT, "Found folder screen from JEI integration");
                        return folderScreenOpt;
                    }
                }
            }
            
            // Check REI integration
            var reiIntegrationOpt = com.enoughfolders.di.DependencyProvider.get(
                com.enoughfolders.integrations.rei.core.REIIntegration.class);
            if (reiIntegrationOpt.isPresent()) {
                var reiIntegration = reiIntegrationOpt.get();
                if (reiIntegration.isAvailable()) {
                    var folderScreenOpt = reiIntegration.getLastFolderScreen();
                    if (folderScreenOpt.isPresent()) {
                        DebugLogger.debug(DebugLogger.Category.INPUT, "Found folder screen from REI integration");
                        return folderScreenOpt;
                    }
                }
            }
        } catch (Exception e) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, 
                "Error accessing integration folder screen: " + e.getMessage());
        }
        
        DebugLogger.debug(DebugLogger.Category.INPUT, "No folder screen found from any source");
        return Optional.empty();
    }
}
