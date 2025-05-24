package com.enoughfolders.integrations.rei.handlers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.ModIntegration;
import com.enoughfolders.util.DebugLogger;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Dedicated handler for adding REI ingredients to folders via keyboard shortcuts.
 */
/**
 * Handles adding ingredients to folders when using REI integration.
 */
public class REIAddToFolderHandler {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private REIAddToFolderHandler() {
        // Utility class should not be instantiated
    }

    /**
     * Adds the currently hovered REI ingredient to the active folder.
     */
    public static void handleAddToFolderKeyPress() {
        EnoughFolders.LOGGER.info("REI Add to Folder handler activated");
        
        try {
            // Check if REI classes exist before doing anything
            Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            
            // Check if REI integration is available via registry
            if (!IntegrationRegistry.isIntegrationAvailable("rei")) {
                EnoughFolders.LOGGER.debug("REI integration not available via registry");
                return;
            }
            
            // Make sure we have an active folder
            FolderManager folderManager = EnoughFolders.getInstance().getFolderManager();
            Optional<Folder> activeFolder = folderManager.getActiveFolder();
            if (activeFolder.isEmpty()) {
                // No active folder, show message to user
                DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                    "No active folder available for adding ingredient");
                return;
            }
            
            String reiIntegrationClassName = "com.enoughfolders.integrations.rei.core.REIIntegration";
            Optional<ModIntegration> reiIntegrationOpt = IntegrationRegistry.getIntegrationByClassName(reiIntegrationClassName);
            
            if (reiIntegrationOpt.isEmpty()) {
                EnoughFolders.LOGGER.debug("REI integration not found in registry by class name");
                return;
            }
            
            // Get the ingredient under mouse using reflection
            ModIntegration reiIntegration = reiIntegrationOpt.get();
            
            // Call getDraggedIngredient via reflection
            Method getDraggedIngredientMethod = reiIntegration.getClass().getMethod("getDraggedIngredient");
            Optional<?> ingredientOpt = (Optional<?>) getDraggedIngredientMethod.invoke(reiIntegration);
            
            EnoughFolders.LOGGER.debug("REI integration found, checking for ingredients under mouse");
            
            // Now process the ingredient if found
            if (ingredientOpt.isPresent()) {
                Object ingredient = ingredientOpt.get();
                EnoughFolders.LOGGER.info("Found REI ingredient under mouse: {}", 
                    ingredient != null ? ingredient.getClass().getName() : "null");
                
                // Store the ingredient
                Method storeIngredientMethod = reiIntegration.getClass().getMethod("storeIngredient", Object.class);
                @SuppressWarnings("unchecked")
                Optional<StoredIngredient> storedIngredientOpt = 
                    (Optional<StoredIngredient>) storeIngredientMethod.invoke(reiIntegration, ingredient);
                
                if (storedIngredientOpt.isPresent()) {
                    StoredIngredient storedIngredient = storedIngredientOpt.get();
                    EnoughFolders.LOGGER.debug("Successfully converted to StoredIngredient: {}", storedIngredient);
                    
                    // Add the ingredient to the active folder
                    Folder folder = activeFolder.get();
                    folderManager.addIngredient(folder, storedIngredient);
                    
                    // Log the action
                    DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                        "Added ingredient to folder '{}'", folder.getName());
                } else {
                    EnoughFolders.LOGGER.error("Failed to convert REI ingredient to StoredIngredient");
                }
            } else {
                EnoughFolders.LOGGER.debug("No REI ingredient found under mouse cursor");
            }
        } catch (ClassNotFoundException e) {
            // REI is not available, that's fine
            EnoughFolders.LOGGER.debug("REI classes not found, skipping REI integration");
        } catch (Exception e) {
            // Something went wrong with reflection or REI API
            EnoughFolders.LOGGER.error("Error interacting with REI runtime", e);
        }
    }
}
