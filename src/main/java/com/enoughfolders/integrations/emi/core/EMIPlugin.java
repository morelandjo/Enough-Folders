package com.enoughfolders.integrations.emi.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.event.ClientEventHandler;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

/**
 * EMI Plugin for Enough Folders integration.
 * This class implements EMI exclusion zones to prevent EMI from overlapping the folder GUI.
 * 
 * The actual EMI plugin registration is handled through the @EmiEntrypoint annotation
 * in the EMIPluginEntrypoint class.
 */
public class EMIPlugin {
    
    private static boolean registered = false;
    
    /**
     * Register the plugin with EMI - this just initializes our handlers.
     * The actual EMI plugin registration happens through EMIPluginEntrypoint.
     */
    public static void register() {
        if (registered) {
            return;
        }
        
        try {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Initializing EMI handlers for Enough Folders", ""
            );
            
            // Register our drag and drop handlers
            registerDragDropHandlers();
            
            registered = true;
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI handler initialization complete", ""
            );
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to initialize EMI handlers", e);
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI handler initialization failed: {}", e.getMessage()
            );
        }
    }
    
    /**
     * Register drag and drop handlers with EMI.
     */
    private static void registerDragDropHandlers() {
        try {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Registering EMI drag and drop handlers", ""
            );
            
            // EMI drag and drop handlers are now registered through EMIPluginEntrypoint
            // via the official EMI plugin API system
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI drag and drop handlers registered", ""
            );
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error registering EMI drag and drop handlers: {}", e.getMessage()
            );
        }
    }
    
    /**
     * Get the folder screen exclusion area for EMI.
     * This method is called by the EMI plugin to get exclusion zones.
     */
    public static void addFolderExclusionArea(Object screen, Object consumer) {
        if (!(screen instanceof AbstractContainerScreen<?>)) {
            return;
        }
        
        AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;
        ClientEventHandler.getFolderScreen(containerScreen).ifPresent(folderScreen -> {
            try {
                // Get the folder screen area
                Rect2i screenArea = folderScreen.getScreenArea();
                
                // Create a Bounds object using reflection
                Class<?> boundsClass = Class.forName("dev.emi.emi.api.widget.Bounds");
                java.lang.reflect.Constructor<?> boundsConstructor = boundsClass.getConstructor(int.class, int.class, int.class, int.class);
                Object bounds = boundsConstructor.newInstance(
                    screenArea.getX() - 2,
                    screenArea.getY() - 2,
                    screenArea.getWidth() + 4,
                    screenArea.getHeight() + 4
                );
                
                // Call the consumer with the bounds
                java.lang.reflect.Method acceptMethod = consumer.getClass().getMethod("accept", Object.class);
                acceptMethod.invoke(consumer, bounds);
                
                DebugLogger.debugValues(
                    DebugLogger.Category.INTEGRATION,
                    "EMI exclusion zone added for folder screen at ({}, {}) with size {}x{}",
                    screenArea.getX(), screenArea.getY(), screenArea.getWidth(), screenArea.getHeight()
                );
                
            } catch (Exception e) {
                DebugLogger.debugValue(
                    DebugLogger.Category.INTEGRATION,
                    "Error creating EMI bounds: {}", e.getMessage()
                );
            }
        });
    }
    
    /**
     * Check if the plugin is registered.
     */
    public static boolean isRegistered() {
        return registered;
    }
}
