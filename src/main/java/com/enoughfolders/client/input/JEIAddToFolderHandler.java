package com.enoughfolders.client.input;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.ModIntegration;
import com.enoughfolders.util.DebugLogger;

import java.util.Optional;

/**
 * Dedicated handler for adding JEI ingredients to folders via keyboard shortcuts.
 * This class uses reflection to avoid direct JEI class references in imports.
 */
public class JEIAddToFolderHandler {

    /**
     * Handles the key press to add a JEI ingredient to the active folder.
     * Safe to call even if JEI is not present.
     */
    public static void handleAddToFolderKeyPress() {
        DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEI Add to Folder handler activated");
        
        // Make sure we have an active folder
        FolderManager folderManager = EnoughFolders.getInstance().getFolderManager();
        Optional<Folder> activeFolder = folderManager.getActiveFolder();
        if (activeFolder.isEmpty()) {
            // No active folder, but don't display a message
            DebugLogger.debug(DebugLogger.Category.INPUT, "No active folder available for adding ingredient");
            return;
        }
        
        DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, "Active folder found: {}", activeFolder.get().getName());
        
        // Use reflection to safely access JEI integration without direct imports
        try {
            // Check if JEI classes exist first
            Class.forName("mezz.jei.api.runtime.IJeiRuntime");
            
            // Get the JEI integration using the class name
            String jeiIntegrationClassName = "com.enoughfolders.integrations.jei.core.JEIIntegration";
            Optional<ModIntegration> jeiIntegrationOpt = IntegrationRegistry.getIntegrationByClassName(jeiIntegrationClassName);
            
            if (jeiIntegrationOpt.isPresent()) {
                Object jeiIntegration = jeiIntegrationOpt.get();
                
                // Call getJeiRuntime method via reflection
                java.lang.reflect.Method getJeiRuntimeMethod = jeiIntegration.getClass().getMethod("getJeiRuntime");
                Optional<?> jeiRuntimeOpt = (Optional<?>) getJeiRuntimeMethod.invoke(jeiIntegration);
                
                if (jeiRuntimeOpt.isPresent()) {
                    Object jeiRuntime = jeiRuntimeOpt.get();
                    DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEI runtime found, checking for ingredients under mouse");
                    
                    // Now use reflection to access JEI APIs
                    processJeiRuntimeWithReflection(jeiRuntime, jeiIntegration, activeFolder.get());
                } else {
                    DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEI runtime not available");
                }
            } else {
                DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEI integration not available");
            }
        } catch (ClassNotFoundException e) {
            // JEI is not present in the classpath, this is totally ok
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEI classes not found, skipping JEI integration");
        } catch (Exception e) {
            // Something went wrong with reflection or JEI API
            EnoughFolders.LOGGER.error("Error interacting with JEI runtime", e);
            DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, "Error interacting with JEI runtime: {}", e.getMessage());
        }
    }
    
    /**
     * Process the JEI runtime using reflection to avoid direct dependencies
     */
    private static void processJeiRuntimeWithReflection(Object jeiRuntime, Object jeiIntegration, Folder activeFolder) 
            throws Exception {
        // Get the ingredient list overlay
        java.lang.reflect.Method getIngredientListOverlayMethod = jeiRuntime.getClass().getMethod("getIngredientListOverlay");
        Object ingredientListOverlay = getIngredientListOverlayMethod.invoke(jeiRuntime);
        
        // Check if ingredient list is displayed
        java.lang.reflect.Method isListDisplayedMethod = ingredientListOverlay.getClass().getMethod("isListDisplayed");
        boolean isListDisplayed = (Boolean) isListDisplayedMethod.invoke(ingredientListOverlay);
        
        if (!isListDisplayed) {
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEI ingredient list overlay is not displayed");
            return;
        }
        
        // Get ingredient under mouse
        java.lang.reflect.Method getIngredientUnderMouseMethod = ingredientListOverlay.getClass().getMethod("getIngredientUnderMouse");
        Optional<?> ingredientOpt = (Optional<?>) getIngredientUnderMouseMethod.invoke(ingredientListOverlay);
        
        if (ingredientOpt.isPresent()) {
            Object typedIngredient = ingredientOpt.get();
            java.lang.reflect.Method getIngredientMethod = typedIngredient.getClass().getMethod("getIngredient");
            Object rawIngredient = getIngredientMethod.invoke(typedIngredient);
            
            DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, "Found ingredient under mouse: {}", rawIngredient.getClass().getName());
            
            // Store the ingredient
            java.lang.reflect.Method storeIngredientMethod = jeiIntegration.getClass().getMethod("storeIngredient", Object.class);
            Optional<?> storedIngredientOpt = (Optional<?>) storeIngredientMethod.invoke(jeiIntegration, rawIngredient);
            
            if (storedIngredientOpt.isPresent()) {
                Object storedIngredient = storedIngredientOpt.get();
                if (storedIngredient instanceof StoredIngredient) {
                    DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                        "Successfully converted to StoredIngredient: {}", storedIngredient);
                    
                    // Add the ingredient to the active folder
                    EnoughFolders.getInstance().getFolderManager().addIngredient(activeFolder, (StoredIngredient)storedIngredient);
                    
                    // Log the action but don't display a message to the player
                    DebugLogger.debugValue(DebugLogger.Category.INPUT, 
                        "Added ingredient to folder '{}'", activeFolder.getName());
                } else {
                    EnoughFolders.LOGGER.error("Stored ingredient is not of expected type: {}", 
                        storedIngredient != null ? storedIngredient.getClass().getName() : "null");
                    DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                        "Stored ingredient is not of expected type: {}", 
                        storedIngredient != null ? storedIngredient.getClass().getName() : "null");
                }
            } else {
                DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                    "Failed to convert ingredient to StoredIngredient: {}", rawIngredient);
            }
        } else {
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "No ingredient found under mouse cursor");
        }
    }
}