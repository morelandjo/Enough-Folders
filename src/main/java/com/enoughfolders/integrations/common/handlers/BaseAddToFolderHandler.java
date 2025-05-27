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
     * Handles the key press to add an ingredient to the active folder.
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
                player.sendSystemMessage(Component.translatable("enoughfolders.error.runtime_interaction", integrationName));
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
}
