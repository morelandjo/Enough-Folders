package com.enoughfolders.integrations.rei.gui.renderers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.integrations.rei.gui.handlers.REIRecipeGuiHandler;
import com.enoughfolders.util.DebugLogger;
import com.enoughfolders.util.DebugLogger.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Optional;

/**
 * Handles rendering the folder UI on REI recipe screens.
 */
@EventBusSubscriber(modid = EnoughFolders.MOD_ID, value = Dist.CLIENT)
public class REIRecipeScreenRenderer {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private REIRecipeScreenRenderer() {
        // Utility class should not be instantiated
    }
    /**
     * Flag to check if REI is available
     */
    private static boolean reiAvailable = false;
    
    /**
     * Names of REI recipe GUI classes
     */
    private static final String[] REI_SCREEN_CLASSES = {
        "me.shedaniel.rei.impl.client.gui.screen.DefaultDisplayViewingScreen",
        "me.shedaniel.rei.impl.client.view.ViewsScreen",
        "me.shedaniel.rei.impl.client.gui.widget.EntryWidget",
        "me.shedaniel.rei.impl.client.gui.widget.favorites.FavoritesListWidget",
        "me.shedaniel.rei.impl.client.gui.widget.EntryListWidget"
    };
    
    static {
        // Check if REI's classes are available
        try {
            Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            reiAvailable = true;
            DebugLogger.debug(Category.REI_INTEGRATION, "REIRecipeScreenRenderer registered for REI recipe overlay functionality");
        } catch (ClassNotFoundException e) {
            reiAvailable = false;
            DebugLogger.debug(Category.REI_INTEGRATION, "REI classes not found, disabling REI recipe overlay functionality");
        }
    }

    // Track screen dimensions to detect changes
    private static int lastScreenWidth = 0;
    private static int lastScreenHeight = 0;

    /**
     * Event handler for post-render events on screens.
     *
     * @param event The screen render post event
     */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        // Skip if REI is not available
        if (!reiAvailable) {
            return;
        }
        
        Screen screen = event.getScreen();
        String screenClassName = screen != null ? screen.getClass().getName() : "null";
        
        // Log screen classes periodically to help identify REI screens
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.tickCount % 100 == 0) {
            DebugLogger.debugValue(Category.REI_INTEGRATION, "Current screen class: {}", screenClassName);
        }
        
        try {
            // Check if the screen is a REI recipe screen
            if (!isREIScreen(screen)) {
                return; // Not a REI recipes screen
            }
            
            DebugLogger.debugValue(Category.REI_INTEGRATION, "Processing render event for REI recipe screen: {}", screenClassName);
            
            // Get current screen dimensions
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            
            // Check for screen resize
            boolean screenResized = (screenWidth != lastScreenWidth || screenHeight != lastScreenHeight);
            if (screenResized) {
                DebugLogger.debug(Category.REI_INTEGRATION, 
                    "Screen dimensions changed: " + lastScreenWidth + "x" + lastScreenHeight + 
                    " -> " + screenWidth + "x" + screenHeight);
                
                lastScreenWidth = screenWidth;
                lastScreenHeight = screenHeight;
                
                // Force reinitialization of folder screen due to size change
                REIRecipeGuiHandler.reinitLastFolderScreen();
            }
            
            // Get the folder screen associated with this REI screen
            Optional<FolderScreen> folderScreenOpt = REIRecipeGuiHandler.getLastFolderScreen();
            if (folderScreenOpt.isPresent()) {
                FolderScreen folderScreen = folderScreenOpt.get();
                
                // Debug logging to help track UI state
                DebugLogger.debugValues(Category.REI_INTEGRATION,
                    "Rendering folder screen on REI screen. Visible: {}, Position: {}x{}, Size: {}x{}", 
                    folderScreen.isVisible(0, 0),
                    folderScreen.getScreenArea().getX(), 
                    folderScreen.getScreenArea().getY(),
                    folderScreen.getScreenArea().getWidth(), 
                    folderScreen.getScreenArea().getHeight());
                
                // Render the folder screen
                GuiGraphics guiGraphics = event.getGuiGraphics();
                folderScreen.render(guiGraphics, 
                    event.getMouseX(), 
                    event.getMouseY(), 
                    event.getPartialTick());
            } else {
                DebugLogger.debug(Category.REI_INTEGRATION, "No folder screen available for REI recipe screen");
            }
        } catch (Exception e) {
            // Log other errors but don't crash
            EnoughFolders.LOGGER.error("Error in REI recipe screen rendering", e);
            DebugLogger.debugValue(Category.REI_INTEGRATION, "Error in REI recipe screen rendering: {}", e.getMessage());
        }
    }
    
    /**
     * Event handler for mouse click on REI screens.
     *
     * @param event The mouse button pressed event
     */
    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        // Skip if REI is not available
        if (!reiAvailable) {
            return;
        }
        
        Screen screen = event.getScreen();
        
        try {
            // Check if the screen is a REI recipe screen
            if (!isREIScreen(screen)) {
                return; // Not a REI recipes screen
            }
            
            // Get the folder screen associated with this REI screen
            Optional<FolderScreen> folderScreenOpt = REIRecipeGuiHandler.getLastFolderScreen();
            if (folderScreenOpt.isPresent()) {
                FolderScreen folderScreen = folderScreenOpt.get();
                
                // Check if the mouse click is inside the folder screen
                if (folderScreen.isVisible(event.getMouseX(), event.getMouseY())) {
                    // Process the mouse click in the folder screen
                    if (folderScreen.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
                        // If the folder screen handled the mouse click, cancel the event
                        DebugLogger.debugValues(Category.REI_INTEGRATION, "Folder screen handled mouse click at {},{}", 
                            event.getMouseX(), event.getMouseY());
                        event.setCanceled(true);
                    }
                }
            }
        } catch (Exception e) {
            // Log other errors but don't crash
            EnoughFolders.LOGGER.error("Error in REI mouse click handling", e);
            DebugLogger.debugValue(Category.REI_INTEGRATION, "Error in REI mouse click handling: {}", e.getMessage());
        }
    }
    
    /**
     * Checks if the given screen is a REI recipe screen.
     *
     * @param screen The screen to check
     * @return true if it's a REI recipe screen, false otherwise
     */
    private static boolean isREIScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        
        String className = screen.getClass().getName();
        
        // Check against known REI screen class names
        for (String reiClass : REI_SCREEN_CLASSES) {
            if (className.equals(reiClass)) {
                return true;
            }
        }
        
        // Also check if the class name contains certain REI-specific fragments
        boolean isREIScreen = className.contains("shedaniel.rei") && 
               (className.contains("RecipeScreen") || 
                className.contains("ViewSearchBuilder") || 
                className.contains("ViewsScreen") ||
                className.contains("DefaultDisplayViewingScreen"));
                
        if (isREIScreen) {
            DebugLogger.debugValue(Category.REI_INTEGRATION, 
                "Detected REI recipe screen: {}", className);
        }
        
        return isREIScreen;
    }
}
