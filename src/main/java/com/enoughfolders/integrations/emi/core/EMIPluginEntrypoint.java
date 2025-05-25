package com.enoughfolders.integrations.emi.core;

import com.enoughfolders.client.event.ClientEventHandler;
import com.enoughfolders.integrations.emi.gui.handlers.EMIFolderDragDropHandler;
import com.enoughfolders.util.DebugLogger;
import dev.emi.emi.api.EmiExclusionArea;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

import java.util.function.Consumer;

/**
 * EMI Plugin entrypoint for registering exclusion zones.
 */
@EmiEntrypoint
public class EMIPluginEntrypoint implements EmiPlugin {
    
    /**
     * Creates a new EMI plugin entrypoint.
     */
    public EMIPluginEntrypoint() {
        // Default constructor
    }

    @Override
    public void register(EmiRegistry registry) {
        try {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI Plugin register() called", ""
            );
            
            // Register exclusion zones for container screens where folder screens might appear
            registerExclusionZones(registry);
            
            // Register drag and drop handlers for EMI ingredients
            EMIFolderDragDropHandler.register(registry);
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI plugin registration completed successfully", ""
            );
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Failed to register EMI exclusion zones: {}", e.getMessage()
            );
        }
    }
    
    /**
     * Register exclusion zones with EMI to prevent overlap with folder GUI
     */
    private void registerExclusionZones(EmiRegistry registry) {
        // Create an exclusion area for all screens
        EmiExclusionArea<Screen> folderExclusionArea = new EmiExclusionArea<Screen>() {
            @Override
            public void addExclusionArea(Screen screen, Consumer<Bounds> consumer) {
                try {
                    // Check if this screen has a folder screen
                    if (screen instanceof AbstractContainerScreen<?>) {
                        // Handle regular container screens with folder UI
                        AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;
                        
                        ClientEventHandler.getFolderScreen(containerScreen).ifPresent(folderScreen -> {
                            try {
                                // Get the folder screen area
                                Rect2i screenArea = folderScreen.getScreenArea();
                                
                                // Add some padding around the folder screen
                                Bounds bounds = new Bounds(
                                    screenArea.getX() - 2,
                                    screenArea.getY() - 2,
                                    screenArea.getWidth() + 4,
                                    screenArea.getHeight() + 4
                                );
                                
                                consumer.accept(bounds);
                                
                                DebugLogger.debugValues(
                                    DebugLogger.Category.INTEGRATION,
                                    "EMI exclusion zone added for container screen at ({}, {}) with size {}x{}",
                                    screenArea.getX(), screenArea.getY(), screenArea.getWidth(), screenArea.getHeight()
                                );
                                
                            } catch (Exception e) {
                                DebugLogger.debugValue(
                                    DebugLogger.Category.INTEGRATION,
                                    "Error creating EMI exclusion bounds for container: {}", e.getMessage()
                                );
                            }
                        });
                    } else {
                        // Handle EMI recipe/ingredient screens that have saved folder screen
                        String className = screen.getClass().getName();
                        boolean isEMIScreen = className.contains("dev.emi.emi");
                        
                        if (isEMIScreen) {
                            // Check if we have a saved folder screen from EMI integration
                            try {
                                Class<?> emiRecipeManagerClass = Class.forName("com.enoughfolders.integrations.emi.core.EMIRecipeManager");
                                java.lang.reflect.Method getLastFolderScreenMethod = emiRecipeManagerClass.getMethod("getLastFolderScreen");
                                @SuppressWarnings("unchecked")
                                java.util.Optional<Object> folderScreenOpt = (java.util.Optional<Object>) getLastFolderScreenMethod.invoke(null);
                                
                                if (folderScreenOpt.isPresent()) {
                                    Object folderScreen = folderScreenOpt.get();
                                    
                                    // Get the screen area using reflection
                                    java.lang.reflect.Method getScreenAreaMethod = folderScreen.getClass().getMethod("getScreenArea");
                                    Rect2i screenArea = (Rect2i) getScreenAreaMethod.invoke(folderScreen);
                                    
                                    // Add some padding around the folder screen
                                    Bounds bounds = new Bounds(
                                        screenArea.getX() - 2,
                                        screenArea.getY() - 2,
                                        screenArea.getWidth() + 4,
                                        screenArea.getHeight() + 4
                                    );
                                    
                                    consumer.accept(bounds);
                                    
                                    DebugLogger.debugValues(
                                        DebugLogger.Category.INTEGRATION,
                                        "EMI exclusion zone added for recipe screen {} at ({}, {}) with size {}x{}",
                                        className, screenArea.getX(), screenArea.getY(), screenArea.getWidth(), screenArea.getHeight()
                                    );
                                }
                            } catch (Exception e) {
                                DebugLogger.debugValue(
                                    DebugLogger.Category.INTEGRATION,
                                    "Error adding EMI exclusion zone for recipe screen {}: {}", e.getMessage()
                                );
                            }
                        }
                    }
                } catch (Exception e) {
                    DebugLogger.debugValue(
                        DebugLogger.Category.INTEGRATION,
                        "Error in EMI addExclusionArea: {}", e.getMessage()
                    );
                }
            }
        };
        
        // Register the exclusion area for all screens
        registry.addGenericExclusionArea(folderExclusionArea);
        
        DebugLogger.debugValue(
            DebugLogger.Category.INTEGRATION,
            "EMI generic exclusion area registered", ""
        );
    }
}
