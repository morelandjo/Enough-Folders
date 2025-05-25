package com.enoughfolders.integrations.rei.handlers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.di.DependencyProvider;
import com.enoughfolders.integrations.rei.core.REIIntegration;
import com.enoughfolders.util.DebugLogger;

import java.util.Optional;

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
        System.out.println("REI HANDLER: Add to Folder handler activated - SYSTEM PRINT");
        
        try {
            // Check if REI classes exist before doing anything
            System.out.println("REI HANDLER: About to check for REI classes - SYSTEM PRINT");
            Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            System.out.println("REI HANDLER: REI classes found - SYSTEM PRINT");
            
            // Check if REI integration is available via DependencyProvider
            System.out.println("REI HANDLER: About to check DependencyProvider for REI integration - SYSTEM PRINT");
            Optional<REIIntegration> reiIntegrationOpt = DependencyProvider.get(REIIntegration.class)
                .filter(integration -> integration.isAvailable());
                
            if (reiIntegrationOpt.isEmpty()) {
                EnoughFolders.LOGGER.debug("REI integration not available via DependencyProvider");
                System.out.println("REI HANDLER: REI integration not available via DependencyProvider - SYSTEM PRINT");
                return;
            }
            System.out.println("REI HANDLER: REI integration found via DependencyProvider - SYSTEM PRINT");
            
            // Make sure we have an active folder
            FolderManager folderManager = EnoughFolders.getInstance().getFolderManager();
            Optional<Folder> activeFolder = folderManager.getActiveFolder();
            System.out.println("REI HANDLER: Active folder check - present: " + activeFolder.isPresent() + " - SYSTEM PRINT");
            if (activeFolder.isEmpty()) {
                // No active folder, show message to user
                DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                    "No active folder available for adding ingredient");
                System.out.println("REI HANDLER: No active folder available - SYSTEM PRINT");
                return;
            }
            System.out.println("REI HANDLER: Active folder found: " + activeFolder.get().getName() + " - SYSTEM PRINT");
            
            // Get the REI integration instance
            REIIntegration reiIntegration = reiIntegrationOpt.get();
            System.out.println("REI HANDLER: Got REI integration instance - SYSTEM PRINT");
            
            
            // Get the ingredient under mouse directly (no need for reflection since we have the proper instance)
            EnoughFolders.LOGGER.info("About to call getIngredientUnderMouse directly");
            System.out.println("REI HANDLER: About to call getIngredientUnderMouse directly - SYSTEM PRINT");
            EnoughFolders.LOGGER.info("REI integration class: {}", reiIntegration.getClass().getName());
            System.out.println("REI HANDLER: REI integration class: " + reiIntegration.getClass().getName());
            
            // Call getIngredientUnderMouse directly
            Optional<?> ingredientOpt = reiIntegration.getIngredientUnderMouse();
            System.out.println("REI HANDLER: getIngredientUnderMouse direct call completed - SYSTEM PRINT");
            EnoughFolders.LOGGER.info("getIngredientUnderMouse direct call completed successfully");
            
            EnoughFolders.LOGGER.info("getIngredientUnderMouse completed, result: {}", 
                ingredientOpt.isPresent() ? "ingredient found" : "no ingredient");
            System.out.println("REI HANDLER: getIngredientUnderMouse result: " + (ingredientOpt.isPresent() ? "ingredient found" : "no ingredient") + " - SYSTEM PRINT");
            
            EnoughFolders.LOGGER.debug("REI integration found, checking for ingredients under mouse");
            
            // Now process the ingredient if found
            if (ingredientOpt.isPresent()) {
                Object ingredient = ingredientOpt.get();
                EnoughFolders.LOGGER.info("Found REI ingredient under mouse: {}", 
                    ingredient != null ? ingredient.getClass().getName() : "null");
                System.out.println("REI HANDLER: Found REI ingredient under mouse: " + (ingredient != null ? ingredient.getClass().getName() : "null") + " - SYSTEM PRINT");
                
                // Store the ingredient directly
                Optional<StoredIngredient> storedIngredientOpt = reiIntegration.storeIngredient(ingredient);
                System.out.println("REI HANDLER: storeIngredient call completed - SYSTEM PRINT");
                
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
            EnoughFolders.LOGGER.debug("REI classes not found, skipping REI integration");
            System.out.println("REI HANDLER: REI classes not found - SYSTEM PRINT: " + e.getMessage());
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error interacting with REI runtime", e);
            System.out.println("REI HANDLER: GENERIC EXCEPTION - SYSTEM PRINT: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
