package com.enoughfolders.client.input;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Handles keyboard input events.
 */
public class KeyHandler {
    /**
     * Flag to track if JEI is available
     */
    private static boolean jeiAvailable = false;
    
    /**
     * Flag to track if REI is available
     */
    private static boolean reiAvailable = false;
    
    /**
     * Initializes the key handler.
     */
    public static void init() {
        // Check if JEI and REI are available without directly importing their classes
        try {
            Class.forName("mezz.jei.api.runtime.IJeiRuntime");
            jeiAvailable = true;
            EnoughFolders.LOGGER.info("JEI runtime classes found");
        } catch (ClassNotFoundException e) {
            jeiAvailable = false;
            EnoughFolders.LOGGER.info("JEI runtime classes not found");
        }
        
        try {
            Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            reiAvailable = true;
            EnoughFolders.LOGGER.info("REI runtime classes found");
        } catch (ClassNotFoundException e) {
            reiAvailable = false;
            EnoughFolders.LOGGER.info("REI runtime classes not found");
        }
        
        // Register to the event bus
        NeoForge.EVENT_BUS.register(KeyHandler.class);
        EnoughFolders.LOGGER.info("Key handler initialized");
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
            
            // Try both JEI and REI handlers, they will safely no-op if not available
            if (jeiAvailable) {
                tryJeiAddToFolder(event.getScreen());
            }
            
            if (reiAvailable) {
                tryReiAddToFolder(event.getScreen());
            }
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
            
            // Try both JEI and REI handlers, they will safely no-op if not available
            if (jeiAvailable) {
                tryJeiAddToFolder(Minecraft.getInstance().screen);
            }
            
            if (reiAvailable) {
                tryReiAddToFolder(Minecraft.getInstance().screen);
            }
        }
    }
    
    /**
     * Try to add an ingredient to a folder using JEI integration
     */
    private static void tryJeiAddToFolder(Screen screen) {
        try {
            // Use JEIAddToFolderHandler which has access to the JEI classes directly
            JEIAddToFolderHandler.handleAddToFolderKeyPress();
        } catch (Throwable t) {
            // If it fails for any reason, log it but don't crash
            EnoughFolders.LOGGER.error("Failed to handle JEI add to folder", t);
        }
    }
    
    /**
     * Try to add an ingredient to a folder using REI integration
     */
    private static void tryReiAddToFolder(Screen screen) {
        try {
            // Use REIAddToFolderHandler which has access to the REI classes directly
            com.enoughfolders.integrations.rei.handlers.REIAddToFolderHandler.handleAddToFolderKeyPress();
        } catch (Throwable t) {
            // If it fails for any reason, log it but don't crash
            EnoughFolders.LOGGER.error("Failed to handle REI add to folder", t);
        }
    }
}