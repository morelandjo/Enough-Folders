package com.enoughfolders.client.event;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
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
        IntegrationRegistry.initialize();
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
            
            // Connect to REI for recipe viewing integration if available
            connectFolderToREI(folderScreen, containerScreen);
            
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
        // Check if we're transitioning to a JEI recipe screen
        boolean goingToRecipeScreen = false;
        
        // Save current folder screen if transitioning to JEI recipe view
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            
            // Check if JEI is loaded before trying to detect JEI recipe transitions
            try {
                // Only check for JEI transitions if JEI is available
                if (IntegrationRegistry.getIntegration(JEIIntegration.class).isPresent()) {
                    // We need to check if this closure is due to JEI opening a recipe view
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    for (StackTraceElement element : stackTrace) {
                        // Check if JEI recipe classes are in the stack trace
                        if (element.getClassName().contains("mezz.jei") && 
                            (element.getMethodName().contains("show") || 
                             element.getClassName().contains("RecipesGui"))) {
                            goingToRecipeScreen = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                    "Error checking for JEI recipe transition: {}", e.getMessage());
            }
            
            // Remove the folder screen from container map
            FOLDER_SCREENS.remove(containerScreen);
        }
        
        // Only clear the saved folder screen when NOT going to a JEI recipe screen
        if (!goingToRecipeScreen) {
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, 
                "Screen closing but not going to JEI - clearing folder screen");
            // Only try to access JEIRecipeGuiHandler if JEI is available
            if (IntegrationRegistry.getIntegration(JEIIntegration.class).isPresent()) {
                com.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler.clearLastFolderScreen();
            }
        }
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
                // Always save the folder screen before any click processing to ensure it's available
                IntegrationRegistry.getIntegration(JEIIntegration.class).ifPresent(jeiIntegration -> {
                    com.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler.saveLastFolderScreen(folderScreen);
                    DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, 
                        "Saved folder screen in mouse click handler to preserve it during JEI navigation");
                });
                
                // Process the click if it's in the folder UI area
                if (folderScreen.isVisible(event.getMouseX(), event.getMouseY())) {
                    if (folderScreen.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
                        event.setCanceled(true);
                    }
                }
            }
        }
        else {
            // Check for JEI recipe GUI screen without directly referencing the class
            // to avoid ClassNotFoundException if JEI is not installed
            try {
                // Only try to access JEI classes if JEI integration is available
                Optional<JEIIntegration> jeiIntegration = IntegrationRegistry.getIntegration(JEIIntegration.class);
                if (jeiIntegration.isPresent() && 
                    com.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler.getLastFolderScreen().isPresent()) {
                    // Use reflection to avoid direct class reference
                    Class<?> recipesGuiClass = Class.forName("mezz.jei.api.runtime.IRecipesGui");
                    if (recipesGuiClass.isInstance(event.getScreen())) {
                        boolean handled = com.enoughfolders.integrations.jei.drag.managers.RecipeGuiManager.handleMouseClick(
                            event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton());
                        if (handled) {
                            event.setCanceled(true);
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                // JEI is not installed, ignore silently
            }
            
            // Check for REI recipe GUI screen
            try {
                // Check if we have a saved folder screen from REI
                Optional<FolderScreen> folderScreenOpt = 
                    com.enoughfolders.integrations.rei.gui.handlers.REIRecipeGuiHandler.getLastFolderScreen();
                
                if (folderScreenOpt.isPresent()) {
                    // Check screen class name to see if it's a REI recipe screen
                    String screenClassName = event.getScreen().getClass().getName();
                    if (screenClassName.contains("shedaniel.rei") && 
                        (screenClassName.contains("RecipeScreen") || screenClassName.contains("ViewSearchBuilder"))) {
                        
                        // Handle the click in the folder screen if it's visible
                        FolderScreen folderScreen = folderScreenOpt.get();
                        if (folderScreen.isVisible(event.getMouseX(), event.getMouseY())) {
                            if (folderScreen.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
                                event.setCanceled(true);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // REI is not installed or there was an error, log and continue
                DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                    "Error handling REI recipe screen mouse click: {}", e.getMessage());
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
     * Connect a folder screen to REI for recipe viewing.
     * 
     * @param folderScreen The folder screen to connect
     * @param containerScreen The container screen
     */
    private static void connectFolderToREI(FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen) {
        try {
            // Get REI integration through the registry
            com.enoughfolders.integrations.IntegrationRegistry.getIntegration(
                com.enoughfolders.integrations.rei.core.REIIntegration.class).ifPresent(integration -> {
                    // Create handler if available
                    if (integration.isAvailable()) {
                        com.enoughfolders.integrations.rei.gui.handlers.REIFolderIngredientHandler handler = 
                            new com.enoughfolders.integrations.rei.gui.handlers.REIFolderIngredientHandler(
                                (com.enoughfolders.integrations.rei.core.REIIntegration) integration);
                        
                        // Connect handler to folder screen
                        handler.connectToFolderScreen(folderScreen, containerScreen);
                    }
                });
        } catch (Exception e) {
            // If there's an exception, it might be because REI is not installed
            EnoughFolders.LOGGER.debug("Could not connect folder to REI: {}", e.getMessage());
        }
    }
    
    /**
     * Check if any recipe viewing mod (REI or JEI) is available.
     * 
     * @return true if either REI or JEI is available
     */
    private static boolean isRecipeViewingAvailable() {
        return com.enoughfolders.integrations.RecipeIntegrationHelper.isRecipeViewingAvailable();
    }
}
