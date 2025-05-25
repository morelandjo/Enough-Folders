package com.enoughfolders.integrations.emi.gui.handlers;

import com.enoughfolders.client.event.ClientEventHandler;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.emi.core.EMIIngredientManager;
import com.enoughfolders.util.DebugLogger;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Handles drag and drop operations between EMI and folder screens.
 */
public class EMIFolderDragDropHandler {
    
    private static boolean initialized = false;
    
    /**
     * Register EMI drag and drop handlers.
     */
    public static void register(EmiRegistry registry) {
        if (initialized) {
            return;
        }
        
        try {
            DebugLogger.debug(
                DebugLogger.Category.INTEGRATION,
                "Registering EMI folder drag drop handler"
            );
            
            // Create a bounds-based drag drop handler for all screens
            EmiDragDropHandler<Screen> folderDragDropHandler = new EmiDragDropHandler.BoundsBased<Screen>(screen -> {
                Map<Bounds, Consumer<EmiIngredient>> boundsMap = new java.util.HashMap<>();
                
                // Check if this screen has a folder screen
                if (screen instanceof AbstractContainerScreen<?>) {
                    AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;
                    
                    ClientEventHandler.getFolderScreen(containerScreen).ifPresent(folderScreen -> {
                        try {
                            // Get the folder screen area as a drop target
                            Rect2i screenArea = folderScreen.getScreenArea();
                            Bounds folderBounds = new Bounds(
                                screenArea.getX(),
                                screenArea.getY(),
                                screenArea.getWidth(),
                                screenArea.getHeight()
                            );
                            
                            // Add drop handler for folder screen area
                            boundsMap.put(folderBounds, ingredient -> {
                                handleFolderDrop(ingredient, folderScreen);
                            });
                            
                            // Also add drop targets for individual folder buttons
                            folderScreen.getFolderButtons().forEach(button -> {
                                Bounds buttonBounds = new Bounds(
                                    button.getX(),
                                    button.getY(),
                                    button.getWidth(),
                                    button.getHeight()
                                );
                                
                                boundsMap.put(buttonBounds, ingredient -> {
                                    handleFolderButtonDrop(ingredient, button.getFolder());
                                });
                            });
                            
                            DebugLogger.debugValue(
                                DebugLogger.Category.INTEGRATION,
                                "Added {} EMI drop targets for folder screen",
                                boundsMap.size()
                            );
                            
                        } catch (Exception e) {
                            DebugLogger.debugValue(
                                DebugLogger.Category.INTEGRATION,
                                "Error creating EMI folder drop targets: {}",
                                e.getMessage()
                            );
                        }
                    });
                }
                
                return boundsMap;
            });
            
            // Register the handler for all screens
            registry.addGenericDragDropHandler(folderDragDropHandler);
            
            initialized = true;
            
            DebugLogger.debug(
                DebugLogger.Category.INTEGRATION,
                "EMI folder drag drop handler registered successfully"
            );
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error registering EMI folder drag drop handler: {}",
                e.getMessage()
            );
        }
    }
    
    /**
     * Handle dropping an ingredient onto the folder screen.
     */
    private static void handleFolderDrop(EmiIngredient ingredient, com.enoughfolders.client.gui.FolderScreen folderScreen) {
        try {
            // Get the active folder
            Optional<Folder> activeFolderOpt = com.enoughfolders.EnoughFolders.getInstance()
                .getFolderManager()
                .getActiveFolder();
            
            if (activeFolderOpt.isEmpty()) {
                DebugLogger.debug(
                    DebugLogger.Category.INTEGRATION,
                    "No active folder to drop EMI ingredient into"
                );
                return;
            }
            
            Folder activeFolder = activeFolderOpt.get();
            handleIngredientDrop(ingredient, activeFolder);
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error handling EMI folder drop: {}",
                e.getMessage()
            );
        }
    }
    
    /**
     * Handle dropping an ingredient onto a specific folder button.
     */
    private static void handleFolderButtonDrop(EmiIngredient ingredient, Folder folder) {
        try {
            if (folder == null) {
                DebugLogger.debug(
                    DebugLogger.Category.INTEGRATION,
                    "Cannot drop EMI ingredient on null folder"
                );
                return;
            }
            
            handleIngredientDrop(ingredient, folder);
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error handling EMI folder button drop: {}",
                e.getMessage()
            );
        }
    }
    
    /**
     * Handle dropping an EMI ingredient into a folder.
     */
    private static void handleIngredientDrop(EmiIngredient ingredient, Folder folder) {
        try {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Processing EMI ingredient drop for folder: {}",
                folder.getName()
            );
            
            // Convert EMI ingredient to StoredIngredient
            Optional<StoredIngredient> storedOpt = EMIIngredientManager.storeIngredient(ingredient);
            if (storedOpt.isEmpty()) {
                DebugLogger.debug(
                    DebugLogger.Category.INTEGRATION,
                    "Failed to convert EMI ingredient to StoredIngredient"
                );
                return;
            }
            
            StoredIngredient storedIngredient = storedOpt.get();
            
            // Add to folder
            folder.addIngredient(storedIngredient);
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Successfully added EMI ingredient to folder {}",
                folder.getName()
            );
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error processing EMI ingredient drop: {}",
                e.getMessage()
            );
        }
    }
    
    /**
     * Check if the handler is initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
