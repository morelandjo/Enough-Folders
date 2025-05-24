package com.enoughfolders.integrations.emi.gui.handlers;

import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.integrations.emi.core.EMIIngredientManager;
import com.enoughfolders.integrations.emi.core.EMIRecipeManager;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Handles interactions between EMI and folder screens.
 */
public class EMIFolderIngredientHandler {
    
    private final FolderScreen folderScreen;
    private final AbstractContainerScreen<?> containerScreen;
    
    public EMIFolderIngredientHandler(FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen) {
        this.folderScreen = folderScreen;
        this.containerScreen = containerScreen;
        
        DebugLogger.debugValue(
            DebugLogger.Category.INTEGRATION,
            "Created EMI folder ingredient handler", ""
        );
    }
    
    /**
     * Handle recipe viewing for an ingredient in the folder.
     */
    public void showRecipes(Object ingredient) {
        try {
            EMIRecipeManager.showRecipes(ingredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error showing recipes in EMI folder handler: {}", e.getMessage()
            );
        }
    }
    
    /**
     * Handle usage viewing for an ingredient in the folder.
     */
    public void showUses(Object ingredient) {
        try {
            EMIRecipeManager.showUses(ingredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error showing uses in EMI folder handler: {}", e.getMessage()
            );
        }
    }
    
    /**
     * Check if an object is a valid EMI ingredient.
     */
    public boolean isValidIngredient(Object ingredient) {
        return EMIIngredientManager.isEMIIngredient(ingredient);
    }
    
    /**
     * Get the folder screen.
     */
    public FolderScreen getFolderScreen() {
        return folderScreen;
    }
    
    /**
     * Get the container screen.
     */
    public AbstractContainerScreen<?> getContainerScreen() {
        return containerScreen;
    }
}
