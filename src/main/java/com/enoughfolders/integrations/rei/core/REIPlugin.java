package com.enoughfolders.integrations.rei.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.util.DebugLogger;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * REI plugin for EnoughFolders mod.
 * This class will be detected by REI's plugin system when installed.
 * 
 * Based on the logs, REI is already detecting our mod as a plugin,
 * but we need to make our code compatible with their registration system.
 */
@OnlyIn(Dist.CLIENT)
@REIPluginClient
public class REIPlugin implements REIClientPlugin {
    
    // Flag to track if we've already logged the exclusion zones
    private static boolean exclusionZonesRegistered = false;
    
    // Initialize static field here to make sure it's loaded
    private static final String STATIC_INIT_MESSAGE = initStaticMessage();
    
    private static String initStaticMessage() {
        EnoughFolders.LOGGER.info("[EnoughFolders/REIPlugin] Static initialization method called (LOGGER)");
        EnoughFolders.LOGGER.info("[EnoughFolders/REIPlugin] ClassLoader: " + REIPlugin.class.getClassLoader().getClass().getName());
        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Static initialization method called");
        DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, "Using ClassLoader: {}", REIPlugin.class.getClassLoader().getClass().getName());
        return "INITIALIZED";
    }
    
    static {
        EnoughFolders.LOGGER.info("[EnoughFolders/REIPlugin] Static block reached (LOGGER)");
        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Static block reached");
    }
    
    /**
     * Constructor for REI plugin registration.
     */
    public REIPlugin() {
        EnoughFolders.LOGGER.info("[EnoughFolders/REIPlugin] Constructor called (LOGGER)");
        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Constructor called");
        DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, "Static init message: {}", STATIC_INIT_MESSAGE);
    }
    
    /**
     * Called when the client is starting up and REI is being initialized.
     * This follows the REIPlugin interface specification.
     */
    @Override
    public void preStage(me.shedaniel.rei.api.common.plugins.PluginManager<REIClientPlugin> manager, me.shedaniel.rei.api.common.registry.ReloadStage stage) {
        EnoughFolders.LOGGER.info("[EnoughFolders/REIPlugin] preStage called with stage: " + stage + " (LOGGER)");
        DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
            "REI plugin pre-stage initialization for stage: {}", stage);
    }
    
    /**
     * Called after REI has been initialized.
     * This follows the REIPlugin interface specification.
     */
    @Override
    public void postStage(me.shedaniel.rei.api.common.plugins.PluginManager<REIClientPlugin> manager, me.shedaniel.rei.api.common.registry.ReloadStage stage) {
        EnoughFolders.LOGGER.info("[EnoughFolders/REIPlugin] postStage called with stage: " + stage + " (LOGGER)");
        DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
            "REI plugin post-stage initialization for stage: {}", stage);
        
        // All drag and drop functionality is now implemented via REI's API
        // through proper handlers registered in REIFolderDragProvider.
    }
    
    /**
     * Register our screen handlers and other UI components with REI.
     * 
     * @param registry The screen registry
     */
    @Override
    public void registerScreens(me.shedaniel.rei.api.client.registry.screen.ScreenRegistry registry) {
        EnoughFolders.LOGGER.info("[EnoughFolders/REIPlugin] registerScreens called (LOGGER)");
        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
            "Registering screens for REI integration");
        
        // Register exclusion zones to keep REI from overlapping with folder UI
        registry.exclusionZones().register(
            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class, 
            screen -> {
                List<me.shedaniel.math.Rectangle> areas = new ArrayList<>();
                
                // Get the folder screen area if present
                com.enoughfolders.client.event.ClientEventHandler.getFolderScreen(screen).ifPresent(folderScreen -> {
                    // Get the screen area and convert to REI's Rectangle format
                    net.minecraft.client.renderer.Rect2i screenArea = folderScreen.getScreenArea();
                    
                    // Add the area with a slight buffer to ensure REI doesn't overlap
                    areas.add(new me.shedaniel.math.Rectangle(
                        screenArea.getX() - 2,
                        screenArea.getY() - 2,
                        screenArea.getWidth() + 4,
                        screenArea.getHeight() + 4
                    ));
                });
                
                return areas;
            }
        );
        
        // Note: Drag and drop functionality is now registered through REIFolderDragProvider:
        // - REIFolderDragProvider implements both DraggableStackVisitor and DraggableStackProvider
        // - This provides a complete implementation of REI's drag and drop system in both directions
        
        // Only log once during registration rather than on every render frame
        if (!exclusionZonesRegistered) {
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "Registered exclusion zones for folder UI");
            exclusionZonesRegistered = true;
        }
    }
    
    /**
     * Register transfer handlers for REI.
     * This allows dragging ingredients from REI to our folders.
     * 
     * @param registry The transfer handler registry
     */
    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        EnoughFolders.LOGGER.info("[EnoughFolders/REIPlugin] registerTransferHandlers called (LOGGER)");
        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
            "Registering transfer handlers for REI integration");
        
        // Register our transfer handler for drag and drop
        try {
            Class<?> handlerClass = Class.forName("com.enoughfolders.integrations.rei.handlers.REITransferHandler");
            Object handler = handlerClass.getDeclaredConstructor().newInstance();
            registry.register((me.shedaniel.rei.api.client.registry.transfer.TransferHandler) handler);
            EnoughFolders.LOGGER.info("Successfully registered REI transfer handler");
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "Successfully registered REI transfer handler");
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("[EnoughFolders/REIPlugin] Failed to register REITransferHandler: " + e.getMessage());
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Failed to register REI transfer handler: {}", e.getMessage());
        }
    }
}
