package com.enoughfolders.client.input;

import com.enoughfolders.EnoughFolders;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * Manages keybindings.
 */
public class KeyBindings {
    /**
     * The translation key category for all keybindings.
     */
    private static final String CATEGORY = "key.categories." + EnoughFolders.MOD_ID;
    
    /**
     * Key mapping for adding ingredient to active folder (Shift + A by default).
     */
    public static final KeyMapping ADD_TO_FOLDER = new KeyMapping(
            "key." + EnoughFolders.MOD_ID + ".add_to_folder",
            KeyConflictContext.GUI,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_A),
            CATEGORY
    );
    
    /**
     * Registers all key bindings and input handlers.
     */
    public static void init() {
        EnoughFolders.LOGGER.info("Registering keyboard event handler");
        // Register the key handler class to receive keyboard events
        NeoForge.EVENT_BUS.register(KeyInputHandler.class);
    }
    
    /**
     * Event handler for registering key mappings.
     * 
     * @param event The register key mappings event
     */
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        EnoughFolders.LOGGER.info("Registering add to folder key mapping");
        event.register(ADD_TO_FOLDER);
    }
    
    /**
     * Separate class for handling keyboard input events.
     */
    public static class KeyInputHandler {
        /**
         * Direct keyboard input handler for the key bindings.
         * 
         * @param event The keyboard input event
         */
        @SubscribeEvent
        public static void onKeyInput(net.neoforged.neoforge.client.event.InputEvent.Key event) {
            // We're only interested in key press events (action == 1)
            if (event.getAction() != GLFW.GLFW_PRESS) {
                return;
            }
            
            int keyCode = event.getKey();
            int scanCode = event.getScanCode();
            
            if (ADD_TO_FOLDER.matches(keyCode, scanCode)) {
                EnoughFolders.LOGGER.info("Add to folder keybind pressed (raw input): key={}, scanCode={}", keyCode, scanCode);
                
                RecipeIntegrationHandler.handleAddToFolderKeyPress();
            }
        }
    }
}