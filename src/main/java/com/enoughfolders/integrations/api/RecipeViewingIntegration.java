package com.enoughfolders.integrations.api;

import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.client.gui.FolderScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;
import java.util.Optional;

/**
 * Interface for recipe viewing operations
 */
public interface RecipeViewingIntegration {
    
    /**
     * Shows recipes for the provided ingredient in the recipe GUI.
     *
     * @param ingredient The ingredient to show recipes for
     */
    void showRecipes(Object ingredient);
    
    /**
     * Shows usages for the provided ingredient in the recipe GUI.
     *
     * @param ingredient The ingredient to show uses for
     */
    void showUses(Object ingredient);
    
    /**
     * Connect a folder screen to the recipe viewer for ingredient interactions.
     * 
     * @param folderScreen The folder screen to connect
     * @param containerScreen The container screen the folder is attached to
     */
    void connectToFolderScreen(FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen);
    
    /**
     * Gets the display name of the recipe viewing integration.
     * 
     * @return The display name
     */
    String getDisplayName();
    
    /**
     * Checks if the recipe viewing integration is available.
     * 
     * @return True if the integration is available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Save a folder screen to be used during recipe GUI navigation.
     * 
     * @param folderScreen The folder screen to save
     */
    void saveLastFolderScreen(FolderScreen folderScreen);
    
    /**
     * Clear the saved folder screen when no longer needed.
     */
    void clearLastFolderScreen();
    
    /**
     * Check if the screen being closed is transitioning to a recipe screen for this integration.
     * 
     * @param screen The screen that's being closed
     * @return True if we're transitioning to a recipe screen, false otherwise
     */
    boolean isTransitioningToRecipeScreen(Screen screen);
    
    /**
     * Get the last folder screen saved for recipe GUI navigation.
     * 
     * @return Optional containing the folder screen if available
     */
    Optional<FolderScreen> getLastFolderScreen();
    
    /**
     * Check if the given screen is a recipe screen for this integration.
     * 
     * @param screen The screen to check
     * @return True if it's a recipe screen for this integration, false otherwise
     */
    boolean isRecipeScreen(Screen screen);
    
    /**
     * Creates folder targets that can be used for ingredient drops from this recipe viewer.
     * 
     * @param folderButtons The list of folder buttons to create targets for
     * @return A list of folder targets compatible with this recipe viewer
     */
    List<? extends FolderTarget> createFolderTargets(List<FolderButton> folderButtons);
}
