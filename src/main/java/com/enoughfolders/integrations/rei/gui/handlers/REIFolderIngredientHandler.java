package com.enoughfolders.integrations.rei.gui.handlers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.client.gui.IngredientSlot;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.rei.core.REIIntegration;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.Optional;

/**
 * Handles interactions between folder ingredient slots and REI.
 */
public class REIFolderIngredientHandler {

    private final REIIntegration reiIntegration;

    /**
     * Creates a new REI folder ingredient handler.
     * 
     * @param reiIntegration The REI integration instance
     */
    public REIFolderIngredientHandler(REIIntegration reiIntegration) {
        this.reiIntegration = reiIntegration;
    }

    /**
     * Handle a click on an ingredient slot in a folder.
     *
     * @param slot The ingredient slot that was clicked
     * @param button The mouse button used (0 = left, 1 = right)
     * @param shift Whether shift was held
     * @param ctrl Whether ctrl was held
     * @return true if the click was handled, false otherwise
     */
    public boolean handleIngredientClick(IngredientSlot slot, int button, boolean shift, boolean ctrl) {
        try {
            if (!reiIntegration.isAvailable()) {
                return false;
            }
            
            // Get the stored ingredient from the slot
            StoredIngredient storedIngredient = slot.getIngredient();
            if (storedIngredient == null) {
                return false;
            }
            
            // Convert stored ingredient to REI ingredient
            Optional<?> ingredientOpt = reiIntegration.getIngredientFromStored(storedIngredient);
            if (ingredientOpt.isEmpty()) {
                return false;
            }
            
            Object ingredient = ingredientOpt.get();
            
            // Determine action based on mouse button and modifiers
            if (button == 0) {
                // Left click - show recipes
                reiIntegration.showRecipes(ingredient);
                return true;
            } else if (button == 1) {
                // Right click - show uses
                reiIntegration.showUses(ingredient);
                return true;
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error handling ingredient click: {}", e.getMessage());
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Exception details: {}", e);
        }
        
        return false;
    }
    
    /**
     * Connects this handler to a folder screen.
     *
     * @param folderScreen The folder screen to connect to
     * @param containerScreen The container screen the folder is attached to
     */
    public void connectToFolderScreen(FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen) {
        try {
            if (!reiIntegration.isAvailable()) {
                return;
            }
            
            // Set up click handlers for the folder's ingredient slots
            folderScreen.registerIngredientClickHandler((slot, button, shift, ctrl) -> 
                handleIngredientClick(slot, button, shift, ctrl));
            
            EnoughFolders.LOGGER.debug("Connected REI ingredient handler to folder screen");
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error connecting to folder screen: {}", e.getMessage());
        }
    }
}
