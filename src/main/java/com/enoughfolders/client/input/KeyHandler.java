package com.enoughfolders.client.input;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.event.ClientEventHandler;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Optional;

/**
 * Handles keyboard input events.
 */
public class KeyHandler {
    /**
     * Initializes the key handler.
     */
    public static void init() {
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

        if (KeyBindings.ADD_TO_FOLDER.isDown()) {
            EnoughFolders.LOGGER.info("ADD_TO_FOLDER keybind is down");
            
            EnoughFolders.LOGGER.debug("KeyBindings.ADD_TO_FOLDER key: {}, isDown: {}, consumeClick: {}",
                KeyBindings.ADD_TO_FOLDER.getKey().getValue(),
                KeyBindings.ADD_TO_FOLDER.isDown(),
                KeyBindings.ADD_TO_FOLDER.consumeClick());
                
            handleAddToFolderKeyPress(event.getScreen());
        } else if (KeyBindings.ADD_TO_FOLDER.consumeClick()) {
            EnoughFolders.LOGGER.info("ADD_TO_FOLDER keybind consumed click");
            handleAddToFolderKeyPress(event.getScreen());
        } else {
            if (event.getKeyCode() == KeyBindings.ADD_TO_FOLDER.getKey().getValue() &&
                KeyBindings.ADD_TO_FOLDER.getKeyModifier().isActive(KeyBindings.ADD_TO_FOLDER.getKeyConflictContext())) {
                EnoughFolders.LOGGER.debug("Key matches but consumeClick returned false. Key={}, Modifier={}",
                    KeyBindings.ADD_TO_FOLDER.getKey().getValue(),
                    KeyBindings.ADD_TO_FOLDER.getKeyModifier());
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
            
        if (KeyBindings.ADD_TO_FOLDER.isDown()) {
            EnoughFolders.LOGGER.info("ADD_TO_FOLDER keybind is down (InputEvent.Key)");
            handleAddToFolderKeyPress(Minecraft.getInstance().screen);
        }
    }
    
    /**
     * Adds the currently hovered JEI ingredient to the active folder.
     * 
     * @param screen The current screen where the key was pressed
     */
    private static void handleAddToFolderKeyPress(Screen screen) {
        EnoughFolders.LOGGER.info("Handling add to folder key press on screen: {}", screen.getClass().getName());
        
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            EnoughFolders.LOGGER.debug("Screen is not an AbstractContainerScreen, ignoring");
            return;
        }
        
        // Get the folder screen associated with this container
        Optional<FolderScreen> optFolderScreen = ClientEventHandler.getFolderScreen(containerScreen);
        if (optFolderScreen.isEmpty()) {
            EnoughFolders.LOGGER.debug("No folder screen associated with this container");
            return;
        }
        
        FolderScreen folderScreen = optFolderScreen.get();
        EnoughFolders.LOGGER.debug("Found folder screen: {}", folderScreen);
        
        // Make sure we have an active folder
        FolderManager folderManager = EnoughFolders.getInstance().getFolderManager();
        Optional<Folder> activeFolder = folderManager.getActiveFolder();
        if (activeFolder.isEmpty()) {
            // No active folder, display a message to the user
            Minecraft.getInstance().player.displayClientMessage(
                Component.translatable("enoughfolders.message.no_active_folder"), false);
            DebugLogger.debug(DebugLogger.Category.INPUT, "No active folder available for adding ingredient");
            return;
        }
        
        EnoughFolders.LOGGER.debug("Active folder found: {}", activeFolder.get().getName());
        
        // Get the JEI integration
        IntegrationRegistry.getIntegration(JEIIntegration.class).ifPresentOrElse(jeiIntegration -> {
            EnoughFolders.LOGGER.debug("JEI integration found");
            
            jeiIntegration.getJeiRuntime().ifPresentOrElse(jeiRuntime -> {
                EnoughFolders.LOGGER.debug("JEI runtime found");
                
                // Check if ingredient list overlay is displayed
                if (!jeiRuntime.getIngredientListOverlay().isListDisplayed()) {
                    EnoughFolders.LOGGER.debug("JEI ingredient list overlay is not displayed");
                    return;
                }
                
                // Try to get ingredient under cursor from JEI
                jeiRuntime.getIngredientListOverlay().getIngredientUnderMouse().ifPresentOrElse(typedIngredient -> {
                    EnoughFolders.LOGGER.info("Found ingredient under mouse: {}", typedIngredient.getIngredient().getClass().getName());
                    
                    // Convert the JEI ingredient to a StoredIngredient
                    Object rawIngredient = typedIngredient.getIngredient();
                    jeiIntegration.storeIngredient(rawIngredient).ifPresentOrElse(storedIngredient -> {
                        EnoughFolders.LOGGER.debug("Successfully converted to StoredIngredient: {}", storedIngredient);
                        
                        // Add the ingredient to the active folder
                        Folder folder = activeFolder.get();
                        folderManager.addIngredient(folder, storedIngredient);
                        
                        // Log the action but don't display a message to the player
                        DebugLogger.debugValue(DebugLogger.Category.INPUT, 
                            "Added ingredient to folder '{}'", folder.getName());
                    }, () -> {
                        EnoughFolders.LOGGER.error("Failed to convert ingredient to StoredIngredient: {}", rawIngredient);
                    });
                }, () -> {
                    EnoughFolders.LOGGER.debug("No ingredient found under mouse cursor");
                    
                    try {
                        double mouseX = Minecraft.getInstance().mouseHandler.xpos() * (double)Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double)Minecraft.getInstance().getWindow().getScreenWidth();
                        double mouseY = Minecraft.getInstance().mouseHandler.ypos() * (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double)Minecraft.getInstance().getWindow().getScreenHeight();
                        
                        EnoughFolders.LOGGER.debug("Mouse position: x={}, y={}", mouseX, mouseY);
                    } catch (Exception e) {
                        EnoughFolders.LOGGER.error("Error getting mouse position", e);
                    }
                });
            }, () -> {
                EnoughFolders.LOGGER.debug("JEI runtime not available");
            });
        }, () -> {
            EnoughFolders.LOGGER.debug("JEI integration not available");
        });
    }
}