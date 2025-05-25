package com.enoughfolders.integrations.jei.gui.handlers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.util.DebugLogger;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles JEI recipe GUI interactions for container screens.
 * 
 * @param <T> The type of container screen this handler works with
 */
public class JEIRecipeGuiHandler<T extends AbstractContainerScreen<?>> implements IGuiContainerHandler<T> {
    
    /**
     * Creates a new JEI recipe GUI handler.
     */
    public JEIRecipeGuiHandler() {
        // Default constructor
    }
    // Keep track of the last used folder screen
    private static FolderScreen lastFolderScreen = null;
    // Keep track if the folder screen has been initialized for the recipe screen
    private static boolean folderScreenInitialized = false;
    // Keep track of the last screen size to detect changes
    private static int lastScreenWidth = 0;
    private static int lastScreenHeight = 0;
    
    /**
     * Save the last active folder screen before opening a recipe view
     * @param folderScreen The folder screen to save
     */
    public static void saveLastFolderScreen(FolderScreen folderScreen) {
        lastFolderScreen = folderScreen;
        folderScreenInitialized = false; // Mark as needing initialization
        DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
            "Saved folder screen for recipe view. position: {}x{}, size: {}x{}, visible: {}", 
            folderScreen.getScreenArea().getX(), folderScreen.getScreenArea().getY(), 
            folderScreen.getScreenArea().getWidth(), folderScreen.getScreenArea().getHeight(),
            folderScreen.isVisible(0, 0));
    }
    
    /**
     * Clear the saved folder screen when no longer needed
     */
    public static void clearLastFolderScreen() {
        if (lastFolderScreen != null) {
            EnoughFolders.LOGGER.debug("Clearing saved folder screen");
        }
        lastFolderScreen = null;
        folderScreenInitialized = false;
        lastScreenWidth = 0;
        lastScreenHeight = 0;
    }
    
    /**
     * Force a reinitialization of the folder screen with current dimensions.
     */
    public static void reinitLastFolderScreen() {
        if (lastFolderScreen == null) {
            return;
        }
        
        // Get current screen dimensions
        Screen currentScreen = Minecraft.getInstance().screen;
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        
        // Only reinit if the screen is a JEI recipe GUI
        if (currentScreen instanceof IRecipesGui) {
            DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION, 
                "Forcing reinitialization of folder screen with dimensions {}x{}", 
                screenWidth, screenHeight);
            
            // Reset the initialization flag to force a full reinit
            folderScreenInitialized = false;
            
            // Initialize the folder screen with current dimensions
            lastFolderScreen.init(screenWidth, screenHeight);
            folderScreenInitialized = true;
            lastScreenWidth = screenWidth;
            lastScreenHeight = screenHeight;
        }
    }
    
    /**
     * Get the currently saved folder screen, initializing it for the recipe screen if needed.
     *
     * @return An Optional containing the last used FolderScreen if available, or empty if none exists
     */
    public static Optional<FolderScreen> getLastFolderScreen() {
        if (lastFolderScreen == null) {
            return Optional.empty();
        }
        
        // Get current screen and dimensions
        Screen currentScreen = Minecraft.getInstance().screen;
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        
        // Initialize the folder screen for the recipe GUI if needed or if screen dimensions changed
        if (currentScreen instanceof IRecipesGui && 
            (!folderScreenInitialized || screenWidth != lastScreenWidth || screenHeight != lastScreenHeight)) {
            
            // Log the current screen type to help with debugging
            DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                "Current screen type for JEI exclusion areas: {}", currentScreen.getClass().getName());
                
            lastFolderScreen.init(screenWidth, screenHeight);
            folderScreenInitialized = true;
            lastScreenWidth = screenWidth;
            lastScreenHeight = screenHeight;
        }
        
        return Optional.of(lastFolderScreen);
    }

    @Override
    @Nonnull
    public List<Rect2i> getGuiExtraAreas(@Nonnull T screen) {
        return new ArrayList<>();
    }
}