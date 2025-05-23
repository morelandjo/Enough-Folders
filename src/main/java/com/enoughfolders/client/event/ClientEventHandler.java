package com.enoughfolders.client.event;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.di.DependencyProvider;
import com.enoughfolders.di.IntegrationProviderRegistry;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.api.RecipeViewingIntegration;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles client-side events.
 */
@EventBusSubscriber(modid = EnoughFolders.MOD_ID, value = Dist.CLIENT, bus = Bus.GAME)
public class ClientEventHandler {
    private static final Map<AbstractContainerScreen<?>, FolderScreen> FOLDER_SCREENS = new HashMap<>();
    
    /**
     * Initialize event handler.
     */
    public static void initialize() {
        // Register the FolderManager with the DI system
        DependencyProvider.registerSingleton(FolderManager.class, EnoughFolders.getInstance().getFolderManager());
        
        // Initialize integration registry using the new DI system
        IntegrationProviderRegistry.initialize();
        
        // Initialize bridge to maintain backward compatibility
        com.enoughfolders.di.IntegrationRegistryBridge.initializeBridge();
    }
    
    /**
     * Gets the FolderScreen associated with the given container screen.
     * 
     * @param screen The container screen to get the folder screen for
     * @return Optional containing the folder screen if it exists, or empty if none exists for the given screen
     */
    public static Optional<FolderScreen> getFolderScreen(AbstractContainerScreen<?> screen) {
        return Optional.ofNullable(FOLDER_SCREENS.get(screen));
    }
    
    /**
     * Event handler for screen opening.
     * 
     * @param event The screen opening event
     */
    @SubscribeEvent
    public static void onScreenOpened(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            DebugLogger.debugValue(DebugLogger.Category.GUI_STATE, 
                "Container screen opened: {}", containerScreen.getClass().getName());
            
            // Create a folder screen for container screen
            FOLDER_SCREENS.put(containerScreen, new FolderScreen(containerScreen));
            
            // Initialize the folder screen
            Minecraft minecraft = Minecraft.getInstance();
            int width = minecraft.getWindow().getGuiScaledWidth();
            int height = minecraft.getWindow().getGuiScaledHeight();
            FolderScreen folderScreen = FOLDER_SCREENS.get(containerScreen);
            folderScreen.init(width, height);
            
            // Connect to recipe viewing mods if available
            connectFolderToRecipeViewers(folderScreen, containerScreen);
            
            DebugLogger.debugValues(DebugLogger.Category.GUI_STATE, 
                "Folder screen initialized with width: {}, height: {}", width, height);
        }
    }
    
    /**
     * Event handler for screen closing.
     * 
     * @param event The screen closing event
     */
    @SubscribeEvent
    public static void onScreenClosed(ScreenEvent.Closing event) {
        // Check if we're transitioning to a recipe screen
        boolean goingToRecipeScreen = false;
        Screen currentScreen = event.getScreen();
        
        // Save current folder screen if transitioning to a recipe view
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            // Check all recipe viewing integrations for transitions
            for (RecipeViewingIntegration integration : getRecipeViewingIntegrations()) {
                if (integration.isAvailable() && integration.isTransitioningToRecipeScreen(currentScreen)) {
                    goingToRecipeScreen = true;
                    EnoughFolders.LOGGER.debug("Detected transition to {} recipe screen", integration.getDisplayName());
                    break;
                }
            }
            
            // Remove the folder screen from container map
            FOLDER_SCREENS.remove(containerScreen);
        }
        
        // Only clear the saved folder screens when NOT going to a recipe screen
        if (!goingToRecipeScreen) {
            DebugLogger.debug(DebugLogger.Category.GUI_STATE, 
                "Screen closing but not going to recipe view - clearing folder screens");
            
            // Clear all saved folder screens
            for (RecipeViewingIntegration integration : getRecipeViewingIntegrations()) {
                if (integration.isAvailable()) {
                    integration.clearLastFolderScreen();
                }
            }
        }
    }
    
    /**
     * Get all available recipe viewing integrations.
     * 
     * @return List of recipe viewing integrations
     */
    private static List<RecipeViewingIntegration> getRecipeViewingIntegrations() {
        List<RecipeViewingIntegration> integrations = new ArrayList<>();
        
        // Add JEI integration if available
        IntegrationRegistry.getIntegration(JEIIntegration.class)
            .ifPresent(integrations::add);
        
        // Add REI integration if available
        IntegrationRegistry.getIntegrationByClassName("com.enoughfolders.integrations.rei.core.REIIntegration")
            .ifPresent(integration -> {
                if (integration instanceof RecipeViewingIntegration) {
                    integrations.add((RecipeViewingIntegration) integration);
                }
            });
        
        return integrations;
    }
    
    /**
     * Event handler for screen rendering (post).
     * 
     * @param event The screen render post event
     */
    @SubscribeEvent
    public static void onScreenDrawForeground(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            // Render folder screen overlay for container screens
            FolderScreen folderScreen = FOLDER_SCREENS.get(containerScreen);
            if (folderScreen != null) {
                folderScreen.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
                DebugLogger.debug(DebugLogger.Category.RENDERING, "Rendered folder screen in foreground");
            }
        }
       
    }
    
    /**
     * Event handler for mouse clicks on screens.
     * 
     * @param event The mouse button pressed pre event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            FolderScreen folderScreen = FOLDER_SCREENS.get(containerScreen);
            if (folderScreen != null) {
                // Save the folder screen
                for (RecipeViewingIntegration integration : getRecipeViewingIntegrations()) {
                    if (integration.isAvailable()) {
                        integration.saveLastFolderScreen(folderScreen);
                        DebugLogger.debugValue(DebugLogger.Category.GUI_STATE, 
                            "Saved folder screen in mouse click handler to preserve it during {} navigation", 
                            integration.getDisplayName());
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
            
            // Check for recipe screens
            for (RecipeViewingIntegration integration : getRecipeViewingIntegrations()) {
                if (!integration.isAvailable()) {
                    continue;
                }
                
                try {
                    // Check if this is a recipe screen and we have a saved folder screen
                    if (integration.isRecipeScreen(currentScreen)) {
                        Optional<FolderScreen> folderScreenOpt = integration.getLastFolderScreen();
                        
                        if (folderScreenOpt.isPresent()) {
                            // Handle the click in the folder screen if it's visible
                            FolderScreen folderScreen = folderScreenOpt.get();
                            if (folderScreen.isVisible(event.getMouseX(), event.getMouseY())) {
                                if (folderScreen.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
                                    event.setCanceled(true);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Error handling recipe screen click, log and continue
                    DebugLogger.debugValue(DebugLogger.Category.GUI_STATE, 
                        "Error handling recipe screen mouse click: {}", 
                        e.getMessage());
                }
            }
            
            // Special case for JEI recipe screen click handling with additional functionality
            try {
                Optional<JEIIntegration> jeiIntegration = IntegrationRegistry.getIntegration(JEIIntegration.class);
                if (jeiIntegration.isPresent() && jeiIntegration.get().isRecipeScreen(currentScreen) && 
                    jeiIntegration.get().getLastFolderScreen().isPresent()) {
                    Class<?> recipesGuiClass = Class.forName("mezz.jei.api.runtime.IRecipesGui");
                    if (recipesGuiClass.isInstance(currentScreen)) {
                        boolean handled = com.enoughfolders.integrations.jei.drag.managers.RecipeGuiManager.handleMouseClick(
                            currentScreen, event.getMouseX(), event.getMouseY(), event.getButton());
                        if (handled) {
                            event.setCanceled(true);
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                // JEI is not installed, ignore silently
            } catch (Exception e) {
                DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                    "Error handling JEI recipe manager click: {}", e.getMessage());
            }
        }
    }
    
   
    
    /**
     * Event handler for keyboard input.
     * 
     * @param event The key pressed pre event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            FolderScreen folderScreen = FOLDER_SCREENS.get(containerScreen);
            if (folderScreen != null) {
                // If input box is active (adding folder mode and input box focused),
                // cancel all key events to prevent inventory from closing
                if (folderScreen.isAddingFolder() && folderScreen.isInputFocused()) {
                    // For 'e' key (inventory) and escape key, always cancel the event
                    // 256 = escape key, 69 = 'e' key
                    if (event.getKeyCode() == 69 || event.getKeyCode() == 256) {
                        event.setCanceled(true);
                    }
                }
                
                // Process the key event in the folder screen
                if (folderScreen.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
                    event.setCanceled(true);
                }
            }
        }
    }
    
    /**
     * Event handler for character typed events.
     * 
     * @param event The character typed pre event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            FolderScreen folderScreen = FOLDER_SCREENS.get(containerScreen);
            if (folderScreen != null) {
                if (folderScreen.charTyped(event.getCodePoint(), event.getModifiers())) {
                    event.setCanceled(true);
                }
            }
        }
    }
    
    /**
     * Event handler for screen rendering post-processing.
     * 
     * @param event The screen render post event
     */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            FolderScreen folderScreen = FOLDER_SCREENS.get(containerScreen);
            if (folderScreen != null) {
                // Re-init the screen with current dimensions to handle resize
                Minecraft minecraft = Minecraft.getInstance();
                int width = minecraft.getWindow().getGuiScaledWidth();
                int height = minecraft.getWindow().getGuiScaledHeight();
                folderScreen.init(width, height);
            }
        }
    }
    
    /**
     * Event handler for when the client player fully joins a world.
     * 
     * @param event The client player network logging in event
     */
    @SubscribeEvent
    public static void onClientPlayerLogin(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        com.enoughfolders.util.DebugLogger.debug(
            com.enoughfolders.util.DebugLogger.Category.INITIALIZATION,
            "ClientEventHandler: Client player fully logged in, world is now ready"
        );
        
        Minecraft.getInstance().execute(() -> {
            EnoughFolders.getInstance().getFolderManager().reloadFolders();
        });
    }
    
    /**
     * Event handler for client tick events.
     * 
     * @param event The client tick post event
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Static state moved out to avoid potential issues
        Minecraft minecraft = Minecraft.getInstance();
        
        // Clear screens when leaving a world
        if (minecraft.level == null && !FOLDER_SCREENS.isEmpty()) {
            FOLDER_SCREENS.clear();
        }
    }
     /**
     * Connect a folder screen to available recipe viewing mods
     * 
     * @param folderScreen The folder screen to connect
     * @param containerScreen The container screen
     */
    private static void connectFolderToRecipeViewers(FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen) {
        // Connect to REI if available
        connectToRecipeViewer("rei", folderScreen, containerScreen);
        
        // Connect to JEI if available
        connectToRecipeViewer("jei", folderScreen, containerScreen);
    }
    
    /**
     * Connect a folder screen to a specific recipe viewer.
     * 
     * @param integrationId The ID of the integration to use
     * @param folderScreen The folder screen to connect
     * @param containerScreen The container screen
     */
    private static void connectToRecipeViewer(String integrationId, FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen) {
        try {
            if (IntegrationRegistry.isIntegrationAvailable(integrationId)) {
                IntegrationRegistry.getIntegrationByClassName(getRecipeViewerClassName(integrationId))
                    .ifPresent(integration -> {
                        if (integration instanceof RecipeViewingIntegration) {
                            ((RecipeViewingIntegration) integration).connectToFolderScreen(folderScreen, containerScreen);
                            EnoughFolders.LOGGER.debug("Connected folder screen to {} recipe viewer", integrationId);
                        }
                    });
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.debug("Could not connect folder to {} recipe viewer: {}", integrationId, e.getMessage());
        }
    }
    
    /**
     * Get the class name for a recipe viewer integration.
     * 
     * @param integrationId The ID of the integration
     * @return The fully qualified class name
     */
    private static String getRecipeViewerClassName(String integrationId) {
        if ("rei".equals(integrationId)) {
            return "com.enoughfolders.integrations.rei.core.REIIntegration";
        } else if ("jei".equals(integrationId)) {
            return "com.enoughfolders.integrations.jei.core.JEIIntegration";
        }
        return "";
    }
    

}
