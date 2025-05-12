package com.enoughfolders.integrations.jei.gui.renderers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler;
import com.enoughfolders.util.DebugLogger;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Optional;

/**
 * Handles rendering the folder UI on JEI recipe screens
 */
@EventBusSubscriber(modid = EnoughFolders.MOD_ID, value = Dist.CLIENT)
public class JEIRecipeScreenRenderer {

    static {
        DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEIRecipeScreenRenderer registered for JEI recipe overlay functionality");
    }

    // Track screen dimensions to detect changes
    private static int lastScreenWidth = 0;
    private static int lastScreenHeight = 0;

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (screen instanceof IRecipesGui) {
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "Processing render event for IRecipesGui screen");
            
            // Get current screen dimensions
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            
            // Track dimensions - DebugLogger will handle deduplication automatically
            DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION, 
                "JEI recipe screen dimensions: {}x{}", screenWidth, screenHeight);
            
            // Check if dimensions have changed
            boolean dimensionsChanged = screenWidth != lastScreenWidth || screenHeight != lastScreenHeight;
            
            if (dimensionsChanged) {
                // Force reinit the folder screen
                JEIRecipeGuiHandler.reinitLastFolderScreen();
                
                // Update tracking variables
                lastScreenWidth = screenWidth;
                lastScreenHeight = screenHeight;
                
                DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION,
                    "Refreshing folder screen due to dimension change");
            }
            
            Optional<FolderScreen> folderScreenOpt = JEIRecipeGuiHandler.getLastFolderScreen();
            if (folderScreenOpt.isPresent()) {
                renderFolderScreen(
                    folderScreenOpt.get(), 
                    event.getGuiGraphics(), 
                    event.getMouseX(), 
                    event.getMouseY(), 
                    event.getPartialTick()
                );
            } else {
                DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "No folder screen available to render");
            }
        }
    }
    
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (screen instanceof IRecipesGui) {
            DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION, "Mouse click on IRecipesGui at {},{}", 
                event.getMouseX(), event.getMouseY());
            
            Optional<FolderScreen> folderScreenOpt = JEIRecipeGuiHandler.getLastFolderScreen();
            if (folderScreenOpt.isPresent()) {
                FolderScreen folderScreen = folderScreenOpt.get();
                if (folderScreen.isVisible(event.getMouseX(), event.getMouseY())) {
                    DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "Mouse click inside folder UI area");
                    if (folderScreen.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
                        event.setCanceled(true);
                        DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "Click handled by folder screen");
                    }
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.Pre event) {
        Screen screen = event.getScreen();
        if (screen instanceof IRecipesGui) {
            Optional<FolderScreen> folderScreenOpt = JEIRecipeGuiHandler.getLastFolderScreen();
            if (folderScreenOpt.isPresent()) {
                FolderScreen folderScreen = folderScreenOpt.get();
                if (folderScreen.isVisible(0, 0)) {  // isVisible with dummy params to check if UI is active
                    if (folderScreen.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
                        event.setCanceled(true);
                        DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION, 
                            "Key press handled by folder screen: keyCode={}, scanCode={}", 
                            event.getKeyCode(), event.getScanCode());
                    }
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        Screen screen = event.getScreen();
        if (screen instanceof IRecipesGui) {
            Optional<FolderScreen> folderScreenOpt = JEIRecipeGuiHandler.getLastFolderScreen();
            if (folderScreenOpt.isPresent()) {
                FolderScreen folderScreen = folderScreenOpt.get();
                if (folderScreen.isVisible(0, 0)) {  // isVisible with dummy params to check if UI is active
                    if (folderScreen.charTyped(event.getCodePoint(), event.getModifiers())) {
                        event.setCanceled(true);
                        DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                            "Character typed handled by folder screen: codePoint={}", 
                            event.getCodePoint());
                    }
                }
            }
        }
    }

    private static void renderFolderScreen(FolderScreen folderScreen, GuiGraphics guiGraphics, 
                                          int mouseX, int mouseY, float partialTick) {
        try {
            folderScreen.render(guiGraphics, mouseX, mouseY, partialTick);
        } catch (Exception e) {
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "Error rendering folder overlay on JEI recipe screen: " + e.getMessage());
        }
    }
}