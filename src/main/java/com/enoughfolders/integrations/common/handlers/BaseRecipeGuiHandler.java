package com.enoughfolders.integrations.common.handlers;

import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.Optional;

/**
 * Base handler for recipe GUI tracking and folder screen connection handling.
 * 
 * @param <T> The type of recipe screen this handler manages
 */
public abstract class BaseRecipeGuiHandler<T extends Screen> {
    
    /**
     * Keep track of the last used folder screen
     */
    protected static FolderScreen lastFolderScreen = null;
    
    /**
     * Keep track if the folder screen has been initialized for the recipe screen
     */
    protected static boolean folderScreenInitialized = false;
    
    /**
     * Keep track of the last screen width to detect changes
     */
    protected static int lastScreenWidth = 0;
    
    /**
     * Keep track of the last screen height to detect changes
     */
    protected static int lastScreenHeight = 0;
    
    /**
     * Default constructor for BaseRecipeGuiHandler.
     */
    protected BaseRecipeGuiHandler() {
        // Default constructor
    }
    
    /**
     * Save the last active folder screen before opening a recipe view.
     * 
     * @param folderScreen The folder screen to save
     */
    public static void saveLastFolderScreen(FolderScreen folderScreen) {
        lastFolderScreen = folderScreen;
        folderScreenInitialized = false; // Mark as needing initialization
        logFolderScreenSaved(folderScreen);
    }
    
    /**
     * Clear the saved folder screen when no longer needed.
     */
    public static void clearLastFolderScreen() {
        if (lastFolderScreen != null) {
            logFolderScreenCleared();
        }
        lastFolderScreen = null;
        folderScreenInitialized = false;
        lastScreenWidth = 0;
        lastScreenHeight = 0;
    }
    
    /**
     * Get the currently saved folder screen, initializing it for the recipe screen if needed.
     * 
     * @return Optional containing the folder screen if available
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
        if (shouldReinitializeFolderScreen(currentScreen, screenWidth, screenHeight)) {
            initializeFolderScreen(currentScreen, screenWidth, screenHeight);
        }
        
        return Optional.of(lastFolderScreen);
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
        
        // Only reinit if the screen is a recipe GUI
        if (isValidRecipeScreen(currentScreen)) {
            logFolderScreenReinit(screenWidth, screenHeight);
            
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
     * Check if the folder screen should be reinitialized based on current conditions.
     * 
     * @param currentScreen The current screen
     * @param screenWidth Current screen width
     * @param screenHeight Current screen height
     * @return true if reinitialization is needed
     */
    protected static boolean shouldReinitializeFolderScreen(Screen currentScreen, int screenWidth, int screenHeight) {
        return isValidRecipeScreen(currentScreen) && 
               (!folderScreenInitialized || 
                screenWidth != lastScreenWidth || 
                screenHeight != lastScreenHeight);
    }
    
    /**
     * Initialize the folder screen with the given dimensions.
     * 
     * @param currentScreen The current screen
     * @param screenWidth Screen width
     * @param screenHeight Screen height
     */
    protected static void initializeFolderScreen(Screen currentScreen, int screenWidth, int screenHeight) {
        logScreenTypeForInit(currentScreen);
        
        lastFolderScreen.init(screenWidth, screenHeight);
        folderScreenInitialized = true;
        lastScreenWidth = screenWidth;
        lastScreenHeight = screenHeight;
    }
    
    /**
     * Check if the given screen is a valid recipe screen for this integration.
     * 
     * @param screen The screen to check
     * @return true if it's a valid recipe screen
     */
    protected static boolean isValidRecipeScreen(Screen screen) {
        // Default implementation - subclasses should override
        return screen != null;
    }
    
    /**
     * Get the integration name for logging purposes.
     * 
     * @return The integration name
     */
    protected static String getIntegrationName() {
        return "UNKNOWN";
    }
    
    /**
     * Get the debug category for logging.
     * 
     * @return The debug category
     */
    protected static DebugLogger.Category getDebugCategory() {
        return DebugLogger.Category.INTEGRATION;
    }
    
    // Logging helper methods
    
    private static void logFolderScreenSaved(FolderScreen folderScreen) {
        DebugLogger.debugValues(getDebugCategory(),
            "Saved folder screen for {} recipe view. position: {}x{}, size: {}x{}, visible: {}", 
            getIntegrationName(),
            folderScreen.getScreenArea().getX(), folderScreen.getScreenArea().getY(), 
            folderScreen.getScreenArea().getWidth(), folderScreen.getScreenArea().getHeight(),
            folderScreen.isVisible(0, 0));
    }
    
    private static void logFolderScreenCleared() {
        DebugLogger.debug(getDebugCategory(), 
            "Clearing saved folder screen for " + getIntegrationName());
    }
    
    private static void logFolderScreenReinit(int screenWidth, int screenHeight) {
        DebugLogger.debugValues(getDebugCategory(), 
            "Forcing reinitialization of {} folder screen with dimensions {}x{}", 
            getIntegrationName(), screenWidth, screenHeight);
    }
    
    private static void logScreenTypeForInit(Screen currentScreen) {
        DebugLogger.debugValues(getDebugCategory(), 
            "Current screen type for {} exclusion areas: {}", 
            getIntegrationName(),
            currentScreen.getClass().getName());
    }
    
    /**
     * Handle mouse clicks on the folder UI when viewing recipe screens.
     * 
     * @param screen The current recipe screen
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button (0=left, 1=right)
     * @return true if the click was handled
     */
    public static boolean handleMouseClick(Screen screen, double mouseX, double mouseY, int button) {
        Optional<FolderScreen> folderScreenOpt = getLastFolderScreen();
        
        if (folderScreenOpt.isPresent() && isValidRecipeScreen(screen)) {
            FolderScreen folderScreen = folderScreenOpt.get();
            if (folderScreen.isVisible(mouseX, mouseY)) {
                boolean handled = folderScreen.mouseClicked(mouseX, mouseY, button);
                if (handled) {
                    DebugLogger.debug(getDebugCategory(), 
                        "Handled mouse click in folder UI on " + getIntegrationName() + " recipe screen");
                }
                return handled;
            }
        }
        return false;
    }
    
    /**
     * Handle key presses on the folder UI when viewing recipe screens.
     * 
     * @param screen The current recipe screen
     * @param keyCode The key code
     * @param scanCode The scan code
     * @param modifiers The modifiers
     * @return true if the key press was handled
     */
    public static boolean handleKeyPress(Screen screen, int keyCode, int scanCode, int modifiers) {
        if (!isValidRecipeScreen(screen)) {
            return false;
        }
        
        Optional<FolderScreen> folderScreenOpt = getLastFolderScreen();
        if (folderScreenOpt.isPresent()) {
            FolderScreen folderScreen = folderScreenOpt.get();
            if (folderScreen.isVisible(0, 0)) {  // isVisible with dummy params to check if UI is active
                if (folderScreen.keyPressed(keyCode, scanCode, modifiers)) {
                    DebugLogger.debugValues(getDebugCategory(), 
                        "Key press handled by {} folder screen: keyCode={}, scanCode={}", 
                        getIntegrationName(), keyCode, scanCode);
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Handle character typed events on the folder UI when viewing recipe screens.
     * 
     * @param screen The current recipe screen
     * @param codePoint The character code point
     * @param modifiers The modifiers
     * @return true if the character input was handled
     */
    public static boolean handleCharTyped(Screen screen, char codePoint, int modifiers) {
        if (!isValidRecipeScreen(screen)) {
            return false;
        }
        
        Optional<FolderScreen> folderScreenOpt = getLastFolderScreen();
        if (folderScreenOpt.isPresent()) {
            FolderScreen folderScreen = folderScreenOpt.get();
            if (folderScreen.isVisible(0, 0)) {  // isVisible with dummy params to check if UI is active
                if (folderScreen.charTyped(codePoint, modifiers)) {
                    DebugLogger.debugValues(getDebugCategory(), 
                        "Character typed handled by {} folder screen: codePoint={}", 
                        getIntegrationName(), codePoint);
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Render the folder screen on the recipe GUI.
     * 
     * @param graphics The GUI graphics context
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param partialTick Partial tick for rendering
     */
    public static void renderFolderScreen(Object graphics, int mouseX, int mouseY, float partialTick) {
        Optional<FolderScreen> folderScreenOpt = getLastFolderScreen();
        if (folderScreenOpt.isPresent()) {
            try {
                // Cast to GuiGraphics - this is safe as all integrations use GuiGraphics
                net.minecraft.client.gui.GuiGraphics guiGraphics = (net.minecraft.client.gui.GuiGraphics) graphics;
                folderScreenOpt.get().render(guiGraphics, mouseX, mouseY, partialTick);
            } catch (Exception e) {
                DebugLogger.debug(getDebugCategory(), 
                    "Error rendering folder overlay on " + getIntegrationName() + " recipe screen: " + e.getMessage());
            }
        }
    }
}
