package com.enoughfolders.integrations.rei.drag.managers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.rei.core.REIIntegration;
import com.enoughfolders.integrations.rei.gui.handlers.REIFolderIngredientHandler;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.Optional;

/**
 * Manager class to handle connecting REI recipe functionality to folder screens.
 */
public class REIFolderManager {
    // Single instance of the handler to use for all folder screens
    private static REIFolderIngredientHandler ingredientHandler;
    
    /**
     * Connect a folder screen to the REI integration system.
     * This enables showing recipes when clicking on ingredients in the folder.
     * 
     * @param folderScreen The folder screen to connect
     * @param containerScreen The container screen the folder is attached to
     * @return true if connected successfully, false otherwise
     */
    public static boolean connectFolderScreen(FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen) {
        try {
            // Get the REI integration instance
            Optional<REIIntegration> reiIntegration = IntegrationRegistry.getIntegration(REIIntegration.class);
            if (reiIntegration.isEmpty() || !reiIntegration.get().isAvailable()) {
                // REI integration is not available, nothing to connect
                return false;
            }
            
            // Create the ingredient handler if it doesn't exist yet
            if (ingredientHandler == null) {
                ingredientHandler = new REIFolderIngredientHandler(reiIntegration.get());
            }
            
            // Connect the handler to the folder screen
            ingredientHandler.connectToFolderScreen(folderScreen, containerScreen);
            
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "Successfully connected REI folder ingredient handler to folder screen");
            return true;
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error connecting REI folder ingredient handler: {}", e.getMessage());
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Exception details: {}", e);
            return false;
        }
    }
}
