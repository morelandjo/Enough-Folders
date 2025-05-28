package com.enoughfolders.integrations.emi.gui.handlers;

import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.di.IntegrationProviderRegistry;
import com.enoughfolders.integrations.emi.core.EMIIngredientManager;
import com.enoughfolders.integrations.emi.core.EMIIntegration;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.Optional;

/**
 * Handles interactions between EMI and folder screens.
 */
public class EMIFolderIngredientHandler {
    
    private final FolderScreen folderScreen;
    private final AbstractContainerScreen<?> containerScreen;
    
    /**
     * Gets the EMI integration instance.
     */
    private static Optional<EMIIntegration> getEMIIntegration() {
        return IntegrationProviderRegistry.getIntegrationByClassName("com.enoughfolders.integrations.emi.core.EMIIntegration")
            .filter(EMIIntegration.class::isInstance)
            .map(EMIIntegration.class::cast);
    }
    
    /**
     * Creates a new EMI folder ingredient handler.
     * @param folderScreen The folder screen to handle
     * @param containerScreen The container screen associated with the folder
     */
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
     * @param ingredient The ingredient to show recipes for
     */
    public void showRecipes(Object ingredient) {
        try {
            getEMIIntegration().ifPresent(emi -> {
                emi.showRecipes(ingredient);
            });
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error showing recipes in EMI folder handler: {}", e.getMessage()
            );
        }
    }
    
    /**
     * Handle usage viewing for an ingredient in the folder.
     * @param ingredient The ingredient to show uses for
     */
    public void showUses(Object ingredient) {
        try {
            getEMIIntegration().ifPresent(emi -> {
                emi.showUses(ingredient);
            });
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error showing uses in EMI folder handler: {}", e.getMessage()
            );
        }
    }
    
    /**
     * Check if an object is a valid EMI ingredient.
     * @param ingredient The object to check
     * @return true if the object is a valid EMI ingredient, false otherwise
     */
    public boolean isValidIngredient(Object ingredient) {
        return EMIIngredientManager.isEMIIngredient(ingredient);
    }
    
    /**
     * Get the folder screen.
     * @return The folder screen being handled
     */
    public FolderScreen getFolderScreen() {
        return folderScreen;
    }
    
    /**
     * Get the container screen.
     * @return The container screen associated with the folder
     */
    public AbstractContainerScreen<?> getContainerScreen() {
        return containerScreen;
    }
}
