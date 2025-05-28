package com.enoughfolders.client.event;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.integrations.api.RecipeViewingIntegration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;
import java.util.Optional;

/**
 * Handles client-side screen interaction events that must be on the MOD bus.
 */
@EventBusSubscriber(modid = EnoughFolders.MOD_ID, value = Dist.CLIENT)
public class ClientScreenEventHandler {
    
    /**
     * Event handler for mouse clicks on screens.
     * 
     * @param event The mouse button pressed pre event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            Optional<FolderScreen> folderScreenOpt = ClientEventHandler.getFolderScreen(containerScreen);
            if (folderScreenOpt.isPresent()) {
                FolderScreen folderScreen = folderScreenOpt.get();
                
                // Save the folder screen to integrations to preserve state
                List<RecipeViewingIntegration> integrations = ClientEventHandler.getRecipeViewingIntegrations();
                for (RecipeViewingIntegration integration : integrations) {
                    if (integration.isAvailable()) {
                        integration.saveLastFolderScreen(folderScreen);
                    }
                }
                
                // Process the click if it's in the folder UI area
                if (folderScreen.isVisible(event.getMouseX(), event.getMouseY())) {
                    if (folderScreen.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
                        event.setCanceled(true);
                    }
                }
            }
        }
        else {
            Screen currentScreen = event.getScreen();
            
            // Check if we have a folder screen for this recipe screen
            Optional<FolderScreen> folderScreenOpt = ClientEventHandler.getRecipeScreen(currentScreen);
            if (folderScreenOpt.isPresent()) {
                FolderScreen folderScreen = folderScreenOpt.get();
                
                // Save the folder screen to all integrations to preserve state
                List<RecipeViewingIntegration> integrations = ClientEventHandler.getRecipeViewingIntegrations();
                for (RecipeViewingIntegration integration : integrations) {
                    if (integration.isAvailable()) {
                        integration.saveLastFolderScreen(folderScreen);
                    }
                }
                
                // Handle the click in the folder screen if it's visible
                if (folderScreen.isVisible(event.getMouseX(), event.getMouseY())) {
                    if (folderScreen.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
    
    /**
     * Event handler for keyboard input.
     * 
     * @param event The key pressed pre event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Screen currentScreen = event.getScreen();
        FolderScreen folderScreen = null;
        
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            Optional<FolderScreen> folderScreenOpt = ClientEventHandler.getFolderScreen(containerScreen);
            folderScreen = folderScreenOpt.orElse(null);
        } else {
            // Check if this is a recipe screen with a folder GUI
            Optional<FolderScreen> folderScreenOpt = ClientEventHandler.getRecipeScreen(currentScreen);
            folderScreen = folderScreenOpt.orElse(null);
        }
        
        if (folderScreen != null) {
            // If input box is active (adding folder mode and input box focused),
            // cancel all key events to prevent keybinds from triggering and inventory from closing
            if (folderScreen.isAddingFolder() && folderScreen.isInputFocused()) {
                // Cancel ALL key events while typing in the folder name input
                // This prevents other mods' keybinds from triggering (like 't' for search)
                event.setCanceled(true);
            }
            
            // Process the key event in the folder screen
            if (folderScreen.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
                event.setCanceled(true);
            }
        }
    }
    
    /**
     * Event handler for character typed events.
     * 
     * @param event The character typed pre event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        Screen currentScreen = event.getScreen();
        FolderScreen folderScreen = null;
        
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            Optional<FolderScreen> folderScreenOpt = ClientEventHandler.getFolderScreen(containerScreen);
            folderScreen = folderScreenOpt.orElse(null);
        } else {
            // Check if this is a recipe screen with a folder GUI
            Optional<FolderScreen> folderScreenOpt = ClientEventHandler.getRecipeScreen(currentScreen);
            folderScreen = folderScreenOpt.orElse(null);
        }
        
        if (folderScreen != null) {
            if (folderScreen.charTyped(event.getCodePoint(), event.getModifiers())) {
                event.setCanceled(true);
            }
        }
    }
}
