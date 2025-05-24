package com.enoughfolders.integrations.emi.gui.renderers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.integrations.emi.core.EMIRecipeManager;
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
 * Handles rendering the folder UI on EMI recipe screens.
 */
@EventBusSubscriber(modid = EnoughFolders.MOD_ID, value = Dist.CLIENT)
public class EMIRecipeScreenRenderer {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private EMIRecipeScreenRenderer() {
        // Utility class should not be instantiated
    }
    
    /**
     * Flag to check if EMI is available
     */
    private static boolean emiAvailable = false;
    
    /**
     * Names of EMI recipe GUI classes - these are the main EMI recipe viewing screens
     */
    private static final String[] EMI_SCREEN_CLASSES = {
        "dev.emi.emi.screen.RecipeScreen",
        "dev.emi.emi.screen.EmiScreen",
        "dev.emi.emi.screen.EmiScreenBase",
        "dev.emi.emi.screen.EmiRecipeScreen"
    };
    
    static {
        // Check if EMI's classes are available
        try {
            Class.forName("dev.emi.emi.api.EmiApi");
            emiAvailable = true;
            DebugLogger.debug(Category.INTEGRATION, "EMIRecipeScreenRenderer registered for EMI recipe overlay functionality");
        } catch (ClassNotFoundException e) {
            emiAvailable = false;
            DebugLogger.debug(Category.INTEGRATION, "EMI classes not found, disabling EMI recipe overlay functionality");
        }
    }

    // Track screen dimensions to detect changes
    private static int lastScreenWidth = 0;
    private static int lastScreenHeight = 0;

    /**
     * Event handler for post-render events on screens.
     * Renders the folder navigation UI when applicable.
     *
     * @param event The screen render post event
     */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        // Skip if EMI is not available
        if (!emiAvailable) {
            return;
        }
        
        Screen screen = event.getScreen();
        String screenClassName = screen != null ? screen.getClass().getName() : "null";
        
        // Log screen classes periodically to help identify EMI screens
        var player = Minecraft.getInstance().player;
        if (player != null && player.tickCount % 100 == 0) {
            DebugLogger.debugValue(Category.INTEGRATION, "Current screen class: {}", screenClassName);
        }
        
        try {
            // Check if the screen is an EMI recipe screen
            if (!isEMIScreen(screen)) {
                return; // Not an EMI recipes screen
            }
            
            DebugLogger.debugValue(Category.INTEGRATION, "Processing render event for EMI recipe screen: {}", screenClassName);
            
            // Get current screen dimensions
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            
            // Check for screen resize
            boolean screenResized = (screenWidth != lastScreenWidth || screenHeight != lastScreenHeight);
            if (screenResized) {
                DebugLogger.debug(Category.INTEGRATION,
                    "Screen dimensions changed: " + lastScreenWidth + "x" + lastScreenHeight + 
                    " -> " + screenWidth + "x" + screenHeight);
                
                lastScreenWidth = screenWidth;
                lastScreenHeight = screenHeight;
            }
            
            // Get the folder screen associated with this EMI screen
            Optional<FolderScreen> folderScreenOpt = EMIRecipeManager.getLastFolderScreen();
            if (folderScreenOpt.isPresent()) {
                FolderScreen folderScreen = folderScreenOpt.get();
                
                // Render the folder screen
                GuiGraphics guiGraphics = event.getGuiGraphics();
                folderScreen.render(guiGraphics, 
                    (int)Minecraft.getInstance().mouseHandler.xpos(), 
                    (int)Minecraft.getInstance().mouseHandler.ypos(), 
                    event.getPartialTick());
                
                DebugLogger.debug(Category.INTEGRATION, "Rendered folder screen on EMI recipe screen");
            }
        } catch (Exception e) {
            // Log other errors but don't crash
            EnoughFolders.LOGGER.error("Error in EMI recipe screen rendering", e);
            DebugLogger.debug(Category.INTEGRATION, "Error in EMI recipe screen rendering: " + e.getMessage());
        }
    }
    
    /**
     * Event handler for mouse click on EMI screens.
     * Handles navigation button clicks in the folder UI overlay.
     *
     * @param event The mouse button pressed event
     */
    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        // Skip if EMI is not available
        if (!emiAvailable) {
            return;
        }
        
        Screen screen = event.getScreen();
        
        try {
            // Check if the screen is an EMI recipe screen
            if (!isEMIScreen(screen)) {
                return; // Not an EMI recipes screen
            }
            
            // Get the folder screen associated with this EMI screen
            Optional<FolderScreen> folderScreenOpt = EMIRecipeManager.getLastFolderScreen();
            if (folderScreenOpt.isPresent()) {
                FolderScreen folderScreen = folderScreenOpt.get();
                
                // Check if the click was on the folder screen area
                double mouseX = event.getMouseX();
                double mouseY = event.getMouseY();
                
                if (folderScreen.mouseClicked(mouseX, mouseY, event.getButton())) {
                    DebugLogger.debug(Category.INTEGRATION, "EMI recipe screen: folder screen handled mouse click");
                    event.setCanceled(true); // Prevent the click from being processed by EMI
                }
            }
        } catch (Exception e) {
            // Log errors but don't crash
            DebugLogger.debugValue(Category.INTEGRATION, "Error handling EMI recipe screen mouse click: {}", e.getMessage());
        }
    }
    
    /**
     * Checks if the given screen is an EMI recipe screen.
     *
     * @param screen The screen to check
     * @return true if it's an EMI recipe screen, false otherwise
     */
    private static boolean isEMIScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        
        String className = screen.getClass().getName();
        
        // Check against known EMI screen class names
        for (String emiClass : EMI_SCREEN_CLASSES) {
            if (className.equals(emiClass)) {
                return true;
            }
        }
        
        // Also check if the class name contains certain EMI-specific fragments
        boolean isEMIScreen = className.contains("dev.emi.emi") && 
               (className.contains("RecipeScreen") || 
                className.contains("EmiScreen") || 
                className.contains("recipe") ||
                className.contains("Recipe"));
                
        if (isEMIScreen) {
            DebugLogger.debugValue(Category.INTEGRATION, 
                "Detected EMI recipe screen: {}", className);
        }
        
        return isEMIScreen;
    }
}
