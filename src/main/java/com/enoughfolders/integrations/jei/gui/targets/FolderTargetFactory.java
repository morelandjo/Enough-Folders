package com.enoughfolders.integrations.jei.gui.targets;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.data.Folder;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.jei.core.JEIIntegrationCore;
import com.enoughfolders.util.DebugLogger;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler.Target;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Factory for creating JEI targets for folder UI elements.
 */
public class FolderTargetFactory {
    
    /**
     * Creates a target for a folder button.
     *
     * @param <I>           The ingredient type
     * @param area          The area of the button
     * @param folder        The folder this button is for
     * @param notifyTarget  The target to notify when an ingredient is added
     * @return A JEI target for the folder button
     */
    public static <I> Target<I> createFolderButtonTarget(
            Rect2i area, 
            Folder folder, 
            FolderGhostIngredientTarget notifyTarget) {
        
        return new Target<I>() {
            @Override
            public Rect2i getArea() {
                return area;
            }
            
            @Override
            public void accept(I ingredientObj) {
                DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
                    "Folder button target accept() called for {} with ingredient: {}", 
                    folder.getName(), ingredientObj.getClass().getSimpleName());
                
                // Get JEI integration
                IntegrationRegistry.getIntegration(JEIIntegrationCore.class).ifPresent(jeiIntegration -> {
                    // Convert the ingredient to StoredIngredient format
                    jeiIntegration.storeIngredient(ingredientObj)
                        .ifPresent(storedIngredient -> {
                            try {
                                // Add the ingredient to this specific folder
                                EnoughFolders.getInstance().getFolderManager()
                                    .addIngredient(folder, storedIngredient);
                                
                                // Notify the GUI that an ingredient was added
                                notifyTarget.onIngredientAdded();
                                
                                DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION,
                                    "SUCCESS: Ingredient added to folder button: {}", 
                                    folder.getName());
                                
                                if (!folder.isActive()) {
                                    DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION,
                                        "Added ingredient to inactive folder: {}", folder.getName());
                                }
                            } catch (Exception e) {
                                EnoughFolders.LOGGER.error("Error adding ingredient to folder", e);
                            }
                        });
                });
            }
        };
    }
    
    /**
     * Creates a target for the folder content area.
     *
     * @param <I>           The ingredient type
     * @param area          The area of the content section
     * @param folder        The active folder
     * @param notifyTarget  The target to notify when an ingredient is added
     * @return A JEI target for the folder content area
     */
    public static <I> Target<I> createContentAreaTarget(
            Rect2i area, 
            Folder folder, 
            FolderGhostIngredientTarget notifyTarget) {
        
        return new Target<I>() {
            @Override
            public Rect2i getArea() {
                return area;
            }

            @Override
            public void accept(I ingredientObj) {
                DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
                    "Content area accept() called for folder {} with ingredient {}", 
                    folder.getName(), ingredientObj.getClass().getSimpleName());
                
                // Get JEI integration
                IntegrationRegistry.getIntegration(JEIIntegrationCore.class).ifPresent(jeiIntegration -> {
                    // Convert the ingredient to StoredIngredient format
                    jeiIntegration.storeIngredient(ingredientObj)
                        .ifPresent(storedIngredient -> {
                            try {
                                // Add the ingredient to the active folder
                                EnoughFolders.getInstance().getFolderManager()
                                    .addIngredient(folder, storedIngredient);
                                
                                // Notify the GUI that an ingredient was added
                                notifyTarget.onIngredientAdded();
                                
                                DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION,
                                    "Ingredient added to folder grid: {}", folder.getName());
                            } catch (Exception e) {
                                EnoughFolders.LOGGER.error("Error adding ingredient to folder", e);
                            }
                        });
                });
            }
        };
    }
    
    /**
     * Creates all targets for a folder screen.
     *
     * @param <I>           The ingredient type
     * @param folderScreen  The folder screen to create targets for
     * @param notifyTarget  The target to notify when ingredients are added
     * @return A list of all targets for the folder screen
     */
    public static <I> List<Target<I>> createAllTargets(
            FolderScreen folderScreen,
            FolderGhostIngredientTarget notifyTarget) {
        
        List<Target<I>> targets = new ArrayList<>();
        
        // Get the active folder (if any) for content drop area
        Optional<Folder> activeFolder = EnoughFolders.getInstance().getFolderManager().getActiveFolder();
        if (activeFolder.isPresent()) {
            // Get folder drop area
            Rect2i dropArea = folderScreen.getContentDropArea();
            Folder folder = activeFolder.get();
            
            DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
                "Adding content grid drop target for active folder: {}, area: {}x{} at {}x{}", 
                folder.getName(), dropArea.getWidth(), dropArea.getHeight(), 
                dropArea.getX(), dropArea.getY());
            
            // Create a target for the drop area
            targets.add(createContentAreaTarget(dropArea, folder, notifyTarget));
        }

        // Get targets for all visible folder buttons
        List<FolderButtonTarget> folderTargets = folderScreen.getJEIFolderTargets();
        DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
            "Found {} folder button targets", folderTargets.size());
        
        // Create a target for each folder button
        int buttonIndex = 0;
        for (FolderButtonTarget folderTarget : folderTargets) {
            buttonIndex++;
            Rect2i buttonArea = folderTarget.getArea();
            Folder targetFolder = folderTarget.getFolder();
            
            DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
                "Adding folder button target #{}: {}, area: {}x{} at {}x{}", 
                buttonIndex,
                targetFolder.getName(), 
                buttonArea.getWidth(), buttonArea.getHeight(),
                buttonArea.getX(), buttonArea.getY());
            
            // Create a target for this folder button
            targets.add(createFolderButtonTarget(buttonArea, targetFolder, notifyTarget));
        }
        
        return targets;
    }
    
    /**
     * Utility method to check if a point is within a rectangle.
     *
     * @param x      The x coordinate
     * @param y      The y coordinate
     * @param rect   The rectangle to check
     * @return true if the point is inside the rectangle
     */
    public static boolean isPointInRect(double x, double y, Rect2i rect) {
        return x >= rect.getX() && x < rect.getX() + rect.getWidth() && 
               y >= rect.getY() && y < rect.getY() + rect.getHeight();
    }
}