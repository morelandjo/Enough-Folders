package com.enoughfolders.client.input;

import com.enoughfolders.EnoughFolders;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Handles keyboard input events.
 */
public class KeyHandler {
    /**
     * Initializes the key handler.
     */
    public static void init() {
        // Register to the event bus
        NeoForge.EVENT_BUS.register(KeyHandler.class);
        EnoughFolders.LOGGER.info("Key handler initialized");
        
        // Initialize the recipe integration handler
        RecipeIntegrationHandler.init();
    }

    /**
     * Processes key inputs when in a GUI screen.
     *
     * @param event The key pressed post event containing key information
     */
    @SubscribeEvent
    public static void onKeyInput(ScreenEvent.KeyPressed.Post event) {
        EnoughFolders.LOGGER.debug("Key pressed: keyCode={}, scanCode={}, modifiers={}",
            event.getKeyCode(), event.getScanCode(), event.getModifiers());

        if (KeyBindings.ADD_TO_FOLDER.isDown() || KeyBindings.ADD_TO_FOLDER.consumeClick()) {
            EnoughFolders.LOGGER.info("ADD_TO_FOLDER keybind activated");
            
            RecipeIntegrationHandler.handleAddToFolderKeyPress();
        }
    }
    
    /**
     * Fallback handler for keyboard events using the raw input system.
     *
     * @param event The raw keyboard input event
     */
    @SubscribeEvent
    public static void onKeyboardInput(InputEvent.Key event) {
        // Only handle the event if we're in a GUI
        if (Minecraft.getInstance().screen == null) return;
        
        EnoughFolders.LOGGER.debug("InputEvent.Key: keyCode={}, scanCode={}, action={}, modifiers={}",
            event.getKey(), event.getScanCode(), event.getAction(), event.getModifiers());
            
        if (KeyBindings.ADD_TO_FOLDER.isDown() || KeyBindings.ADD_TO_FOLDER.consumeClick()) {
            EnoughFolders.LOGGER.info("ADD_TO_FOLDER keybind activated (raw input)");
            
            RecipeIntegrationHandler.handleAddToFolderKeyPress();
        }
    }
    
}