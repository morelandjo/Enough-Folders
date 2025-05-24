package com.enoughfolders.integrations.emi.handlers;

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
 * Dedicated handler for adding EMI ingredients to folders via keyboard shortcuts.
 */
public class EMIAddToFolderHandler {

    /**
     * Adds the currently hovered EMI ingredient to the active folder.
     */
    public static void handleAddToFolderKeyPress() {
        EnoughFolders.LOGGER.info("EMI Add to Folder handler activated");
        
        try {
            // Check if EMI classes exist before doing anything
            Class.forName("dev.emi.emi.api.EmiApi");
            
            // Check if EMI integration is available via registry
            if (!IntegrationRegistry.isIntegrationAvailable("emi")) {
                EnoughFolders.LOGGER.debug("EMI integration not available via registry");
                return;
            }
            
            // Make sure we have an active folder
            FolderManager folderManager = EnoughFolders.getInstance().getFolderManager();
            Optional<Folder> activeFolder = folderManager.getActiveFolder();
            if (activeFolder.isEmpty()) {
                // No active folder, show message to user
                EnoughFolders.LOGGER.info("No active folder available for adding ingredient - need to create a folder first");
                DebugLogger.debug(DebugLogger.Category.INTEGRATION, 
                    "No active folder available for adding ingredient");
                return;
            }
            
            EnoughFolders.LOGGER.info("Active folder found: {}", activeFolder.get().getName());
            
            String emiIntegrationClassName = "com.enoughfolders.integrations.emi.core.EMIIntegration";
            Optional<ModIntegration> emiIntegrationOpt = IntegrationRegistry.getIntegrationByClassName(emiIntegrationClassName);
            
            if (emiIntegrationOpt.isEmpty()) {
                EnoughFolders.LOGGER.debug("EMI integration not found in registry by class name");
                return;
            }
            
            // Get the ingredient under mouse using reflection
            ModIntegration emiIntegration = emiIntegrationOpt.get();
            
            // Call getIngredientUnderMouse via reflection (EMI uses hovered ingredients, not dragged)
            Method getIngredientUnderMouseMethod = emiIntegration.getClass().getMethod("getIngredientUnderMouse");
            Optional<?> ingredientOpt = (Optional<?>) getIngredientUnderMouseMethod.invoke(emiIntegration);
            
            EnoughFolders.LOGGER.debug("EMI integration found, checking for ingredients under mouse");
            
            // Now process the ingredient if found
            if (ingredientOpt.isPresent()) {
                Object ingredient = ingredientOpt.get();
                EnoughFolders.LOGGER.info("Found EMI ingredient under mouse: {}", 
                    ingredient != null ? ingredient.getClass().getName() : "null");
                
                EnoughFolders.LOGGER.info("About to call storeIngredient method...");
                
                // Store the ingredient
                Method storeIngredientMethod = emiIntegration.getClass().getMethod("storeIngredient", Object.class);
                @SuppressWarnings("unchecked")
                Optional<StoredIngredient> storedIngredientOpt = 
                    (Optional<StoredIngredient>) storeIngredientMethod.invoke(emiIntegration, ingredient);
                
                EnoughFolders.LOGGER.info("storeIngredient method returned, checking result...");
                
                if (storedIngredientOpt.isPresent()) {
                    StoredIngredient storedIngredient = storedIngredientOpt.get();
                    EnoughFolders.LOGGER.info("Successfully converted to StoredIngredient: {}", storedIngredient);
                    
                    // Add the ingredient to the active folder
                    Folder folder = activeFolder.get();
                    EnoughFolders.LOGGER.info("About to add ingredient to folder: {}", folder.getName());
                    
                    folderManager.addIngredient(folder, storedIngredient);
                    
                    EnoughFolders.LOGGER.info("Successfully added ingredient to folder!");
                    
                    // Log the action
                    DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                        "Added ingredient to folder '{}'", folder.getName());
                } else {
                    EnoughFolders.LOGGER.error("Failed to convert EMI ingredient to StoredIngredient - storeIngredient returned empty Optional");
                }
            } else {
                EnoughFolders.LOGGER.debug("No EMI ingredient found under mouse cursor");
            }
        } catch (ClassNotFoundException e) {
            // EMI is not available, that's fine
            EnoughFolders.LOGGER.debug("EMI classes not found, skipping EMI integration");
        } catch (Exception e) {
            // Something went wrong with reflection or EMI API
            EnoughFolders.LOGGER.error("Error interacting with EMI runtime", e);
        }
    }
}
