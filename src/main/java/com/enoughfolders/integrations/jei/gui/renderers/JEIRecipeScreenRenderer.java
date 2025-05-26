package com.enoughfolders.integrations.jei.gui.renderers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.integrations.common.handlers.BaseRecipeGuiHandler;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Optional;

/**
 * Handles rendering the folder UI on JEI recipe screens.
 */
@EventBusSubscriber(modid = EnoughFolders.MOD_ID, value = Dist.CLIENT)
public class JEIRecipeScreenRenderer {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private JEIRecipeScreenRenderer() {
        // Utility class should not be instantiated
    }
    /**
     * Flag to check if JEI is available
     */
    private static boolean jeiAvailable = false;
    
    /**
     * Name of the JEI recipe GUI interface
     */
    private static final String JEI_RECIPES_GUI_CLASS = "mezz.jei.api.runtime.IRecipesGui";
    
    static {
        // Check if JEI's classes are available
        try {
            Class.forName(JEI_RECIPES_GUI_CLASS);
            jeiAvailable = true;
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEIRecipeScreenRenderer registered for JEI recipe overlay functionality");
        } catch (ClassNotFoundException e) {
            jeiAvailable = false;
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEI classes not found, disabling JEI recipe overlay functionality");
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
        // Skip if JEI is not available
        if (!jeiAvailable) {
            return;
        }
        
        Screen screen = event.getScreen();
        
        try {
            // Check if the screen is an instance of IRecipesGui
            Class<?> recipesGuiClass = Class.forName(JEI_RECIPES_GUI_CLASS);
            if (!recipesGuiClass.isInstance(screen)) {
                return; // Not a JEI recipes screen
            }
            
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "Processing render event for IRecipesGui screen");
            
            // Get current screen dimensions
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            
            // Check for screen resize
            boolean screenResized = (screenWidth != lastScreenWidth || screenHeight != lastScreenHeight);
            if (screenResized) {
                DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, 
                    "Screen dimensions changed: " + lastScreenWidth + "x" + lastScreenHeight + 
                    " -> " + screenWidth + "x" + screenHeight);
                
                lastScreenWidth = screenWidth;
                lastScreenHeight = screenHeight;
            }
            
            // Get the folder screen associated with this JEI screen
            Optional<FolderScreen> folderScreenOpt = BaseRecipeGuiHandler.getLastFolderScreen();
            if (folderScreenOpt.isPresent()) {
                FolderScreen folderScreen = folderScreenOpt.get();
                
                // Render the folder screen
                GuiGraphics guiGraphics = event.getGuiGraphics();
                folderScreen.render(guiGraphics, 
                    (int)Minecraft.getInstance().mouseHandler.xpos(), 
                    (int)Minecraft.getInstance().mouseHandler.ypos(), 
                    event.getPartialTick());
            }
        } catch (ClassNotFoundException e) {
            // JEI classes not found, disable this renderer
            jeiAvailable = false;
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEI classes not found, disabling JEI recipe overlay functionality");
        } catch (Exception e) {
            // Log other errors but don't crash
            EnoughFolders.LOGGER.error("Error in JEI recipe screen rendering", e);
        }
    }
}