package com.enoughfolders.client.event;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.di.DependencyProvider;
import com.enoughfolders.di.IntegrationProviderRegistry;
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
    
    /**
     * Creates a new client event handler.
     */
    public ClientEventHandler() {
        // Default constructor
    }
    private static final Map<AbstractContainerScreen<?>, FolderScreen> FOLDER_SCREENS = new HashMap<>();
    private static final Map<Screen, FolderScreen> RECIPE_SCREENS = new HashMap<>();
    
    /**
     * Initialize event handler.
     */
    public static void initialize() {
        // Register the FolderManager
        DependencyProvider.registerSingleton(FolderManager.class, EnoughFolders.getInstance().getFolderManager());
        
        // Initialize integration registry
        IntegrationProviderRegistry.initialize();
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
        Screen newScreen = event.getScreen();
        
        if (newScreen instanceof AbstractContainerScreen<?> containerScreen) {
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
        } else {
            // Check if this is a recipe screen that should have folder GUI
            for (RecipeViewingIntegration integration : getRecipeViewingIntegrations()) {
                if (integration.isAvailable() && integration.isRecipeScreen(newScreen)) {
                    // This is a recipe screen, check if we have a saved folder screen
                    Optional<FolderScreen> savedFolderScreen = integration.getLastFolderScreen();
                    if (savedFolderScreen.isPresent()) {
                        DebugLogger.debug(DebugLogger.Category.GUI_STATE, 
                            "Recipe screen opened, restoring folder screen for " + integration.getDisplayName());
                        
                        // For recipe screens, we need to create a temporary mapping to enable folder rendering
                        // We'll use a special map entry for recipe screens
                        RECIPE_SCREENS.put(newScreen, savedFolderScreen.get());
                        
                        // Re-initialize the folder screen for the new dimensions
                        Minecraft minecraft = Minecraft.getInstance();
                        int width = minecraft.getWindow().getGuiScaledWidth();
                        int height = minecraft.getWindow().getGuiScaledHeight();
                        savedFolderScreen.get().init(width, height);
                    }
                    break;
                }
            }
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
                    System.out.println("DETECTED RECIPE TRANSITION FOR: " + integration.getDisplayName());
                    break;
                }
            }
            
            // Remove the folder screen from container map
            FOLDER_SCREENS.remove(containerScreen);
        } else {
            // Clean up recipe screen mappings
            RECIPE_SCREENS.remove(currentScreen);
            DebugLogger.debugValue(DebugLogger.Category.GUI_STATE, 
                "Screen closing - removed from recipe screens: {}", 
                currentScreen != null ? currentScreen.getClass().getSimpleName() : "null");
            
            // For EMI recipe screens, don't clear immediately since EMI might be transitioning between screens
            if (currentScreen != null && currentScreen.getClass().getName().contains("dev.emi.emi")) {
                DebugLogger.debug(DebugLogger.Category.GUI_STATE, 
                    "EMI recipe screen closing - delaying folder screen cleanup");
                goingToRecipeScreen = true; // Prevent clearing for EMI screens
            }
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
        DependencyProvider.get(JEIIntegration.class)
            .ifPresent(integrations::add);
        
        // Add REI integration if available
        try {
            Class<?> reiIntegrationClass = Class.forName("com.enoughfolders.integrations.rei.core.REIIntegration");
            Optional<?> reiIntegration = DependencyProvider.get(reiIntegrationClass);
            if (reiIntegration.isPresent() && reiIntegration.get() instanceof RecipeViewingIntegration) {
                integrations.add((RecipeViewingIntegration) reiIntegration.get());
            }
        } catch (ClassNotFoundException e) {
            // REI integration not available, ignore
        }
        
        // Add EMI integration if available
        try {
            Class<?> emiIntegrationClass = Class.forName("com.enoughfolders.integrations.emi.core.EMIIntegration");
            Optional<?> emiIntegration = DependencyProvider.get(emiIntegrationClass);
            if (emiIntegration.isPresent() && emiIntegration.get() instanceof RecipeViewingIntegration) {
                integrations.add((RecipeViewingIntegration) emiIntegration.get());
            }
        } catch (ClassNotFoundException e) {
            // EMI integration not available, ignore
        }
        
        return integrations;
    }
    
    /**
     * Event handler for screen rendering.
     * 
     * @param event The screen render post event
     */
    @SubscribeEvent
    public static void onScreenDrawForeground(ScreenEvent.Render.Post event) {
        Screen currentScreen = event.getScreen();
        FolderScreen folderScreen = null;
        
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            // Render folder screen overlay for container screens
            folderScreen = FOLDER_SCREENS.get(containerScreen);
        } else {
            // Check if this is a recipe screen with a saved folder screen
            folderScreen = RECIPE_SCREENS.get(currentScreen);
        }
        
        if (folderScreen != null) {
            folderScreen.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            DebugLogger.debug(DebugLogger.Category.RENDERING, "Rendered folder screen in foreground");
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
            
            // Check if we have a folder screen for this recipe screen
            FolderScreen folderScreen = RECIPE_SCREENS.get(currentScreen);
            if (folderScreen != null) {
                DebugLogger.debugValues(DebugLogger.Category.GUI_STATE, 
                    "Processing mouse click on recipe screen with folder GUI at ({}, {})", 
                    event.getMouseX(), event.getMouseY());
                
                // Save the folder screen to all integrations to preserve state
                for (RecipeViewingIntegration integration : getRecipeViewingIntegrations()) {
                    if (integration.isAvailable()) {
                        integration.saveLastFolderScreen(folderScreen);
                    }
                }
                
                // Handle the click in the folder screen if it's visible
                if (folderScreen.isVisible(event.getMouseX(), event.getMouseY())) {
                    if (folderScreen.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
                        DebugLogger.debug(DebugLogger.Category.GUI_STATE, 
                            "Mouse click handled by folder screen on recipe screen, canceling event");
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
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Screen currentScreen = event.getScreen();
        FolderScreen folderScreen = null;
        
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            folderScreen = FOLDER_SCREENS.get(containerScreen);
        } else {
            // Check if this is a recipe screen with a folder GUI
            folderScreen = RECIPE_SCREENS.get(currentScreen);
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
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        Screen currentScreen = event.getScreen();
        FolderScreen folderScreen = null;
        
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            folderScreen = FOLDER_SCREENS.get(containerScreen);
        } else {
            // Check if this is a recipe screen with a folder GUI
            folderScreen = RECIPE_SCREENS.get(currentScreen);
        }
        
        if (folderScreen != null) {
            if (folderScreen.charTyped(event.getCodePoint(), event.getModifiers())) {
                event.setCanceled(true);
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
        if (minecraft.level == null && (!FOLDER_SCREENS.isEmpty() || !RECIPE_SCREENS.isEmpty())) {
            FOLDER_SCREENS.clear();
            RECIPE_SCREENS.clear();
            DebugLogger.debug(DebugLogger.Category.GUI_STATE, "Cleared all folder screens when leaving world");
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
        
        // Connect to EMI if available
        connectToRecipeViewer("emi", folderScreen, containerScreen);
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
            if (IntegrationProviderRegistry.hasIntegrationWithShortId(integrationId)) {
                // Get integration by class for type safety
                if ("jei".equals(integrationId)) {
                    DependencyProvider.get(JEIIntegration.class)
                        .ifPresent(integration -> {
                            integration.connectToFolderScreen(folderScreen, containerScreen);
                            EnoughFolders.LOGGER.debug("Connected folder screen to {} recipe viewer", integrationId);
                        });
                } else if ("rei".equals(integrationId)) {
                    try {
                        Class<?> reiIntegrationClass = Class.forName("com.enoughfolders.integrations.rei.core.REIIntegration");
                        Optional<?> reiIntegration = DependencyProvider.get(reiIntegrationClass);
                        if (reiIntegration.isPresent() && reiIntegration.get() instanceof RecipeViewingIntegration) {
                            ((RecipeViewingIntegration) reiIntegration.get()).connectToFolderScreen(folderScreen, containerScreen);
                            EnoughFolders.LOGGER.debug("Connected folder screen to {} recipe viewer", integrationId);
                        }
                    } catch (ClassNotFoundException e) {
                        // REI integration not available, ignore
                    }
                } else if ("emi".equals(integrationId)) {
                    try {
                        Class<?> emiIntegrationClass = Class.forName("com.enoughfolders.integrations.emi.core.EMIIntegration");
                        Optional<?> emiIntegration = DependencyProvider.get(emiIntegrationClass);
                        if (emiIntegration.isPresent() && emiIntegration.get() instanceof RecipeViewingIntegration) {
                            ((RecipeViewingIntegration) emiIntegration.get()).connectToFolderScreen(folderScreen, containerScreen);
                            EnoughFolders.LOGGER.debug("Connected folder screen to {} recipe viewer", integrationId);
                        }
                    } catch (ClassNotFoundException e) {
                        // EMI integration not available, ignore
                    }
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.debug("Could not connect folder to {} recipe viewer: {}", integrationId, e.getMessage());
        }
    }
}
