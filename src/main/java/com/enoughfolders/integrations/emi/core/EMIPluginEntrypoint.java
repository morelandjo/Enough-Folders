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
 * This class is automatically discovered by EMI through the @EmiEntrypoint annotation.
 * <p>
 * This class implements the EMI plugin interface and serves as the main entry point
 * for EMI to interact with EnoughFolders.
 * </p>
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
                                    "EMI exclusion zone added for folder screen at ({}, {}) with size {}x{}",
                                    screenArea.getX(), screenArea.getY(), screenArea.getWidth(), screenArea.getHeight()
                                );
                                
                            } catch (Exception e) {
                                DebugLogger.debugValue(
                                    DebugLogger.Category.INTEGRATION,
                                    "Error creating EMI exclusion bounds: {}", e.getMessage()
                                );
                            }
                        });
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
