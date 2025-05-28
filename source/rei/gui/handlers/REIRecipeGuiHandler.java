package com.enoughfolders.integrations.rei.gui.handlers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.Optional;

/**
 * Handler for tracking folder screens when navigating to REI recipe screens.
 */
public class REIRecipeGuiHandler {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private REIRecipeGuiHandler() {
        // Utility class should not be instantiated
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
        folderScreenInitialized = false;
        
        // Force initialization with current screen dimensions to ensure proper display
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        folderScreen.init(screenWidth, screenHeight);
        
        EnoughFolders.LOGGER.info("Saved folder screen for REI recipe view at position: {}x{}, size: {}x{}, visible: {}", 
            folderScreen.getScreenArea().getX(), folderScreen.getScreenArea().getY(), 
            folderScreen.getScreenArea().getWidth(), folderScreen.getScreenArea().getHeight(),
            folderScreen.isVisible(0, 0));
            
        DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
            "Saved folder screen for REI recipe view. position: {}x{}, size: {}x{}, visible: {}", 
            folderScreen.getScreenArea().getX(), folderScreen.getScreenArea().getY(), 
            folderScreen.getScreenArea().getWidth(), folderScreen.getScreenArea().getHeight(),
            folderScreen.isVisible(0, 0));
    }
    
    /**
     * Clear the saved folder screen when no longer needed
     */
    public static void clearLastFolderScreen() {
        if (lastFolderScreen != null) {
            EnoughFolders.LOGGER.debug("Clearing saved REI folder screen");
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
        if (currentScreen == null) {
            return;
        }
        
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        
        // Check if the current screen is a REI recipe screen
        if (isRecipeScreen(currentScreen)) {
            DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION, 
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
        if (currentScreen == null) {
            return Optional.of(lastFolderScreen);
        }
        
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        
        // Initialize the folder screen for the recipe GUI if needed or if screen dimensions changed
        if (isRecipeScreen(currentScreen) &&
            (!folderScreenInitialized || screenWidth != lastScreenWidth || screenHeight != lastScreenHeight)) {
            
            // Log the current screen type to help with debugging
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Current screen type for REI exclusion areas: {}", currentScreen.getClass().getName());
                
            lastFolderScreen.init(screenWidth, screenHeight);
            folderScreenInitialized = true;
            lastScreenWidth = screenWidth;
            lastScreenHeight = screenHeight;
        }
        
        return Optional.of(lastFolderScreen);
    }
    
    /**
     * Check if the given screen is a REI recipe screen.
     * 
     * @param screen The screen to check
     * @return true if it's a REI recipe screen, false otherwise
     */
    private static boolean isRecipeScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        
        // Check if the screen's class name contains "RecipeScreen" or "ViewSearchBuilder"
        String className = screen.getClass().getName();
        boolean isREIScreen = className.contains("shedaniel.rei") && 
               (className.contains("RecipeScreen") || 
                className.contains("ViewSearchBuilder") || 
                className.contains("ViewsScreen") ||
                className.contains("DefaultDisplayViewingScreen"));
                
        if (isREIScreen) {
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Detected REI recipe screen: {}", className);
        }
        
        return isREIScreen;
    }
}
