package com.enoughfolders.integrations.jei.gui.handlers;

import com.enoughfolders.data.Folder;
import com.enoughfolders.di.DependencyProvider;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import com.enoughfolders.integrations.jei.gui.targets.FolderButtonTarget;
import com.enoughfolders.integrations.jei.gui.targets.FolderGhostIngredientTarget;
import com.enoughfolders.integrations.jei.gui.targets.FolderTargetFactory;
import com.enoughfolders.util.DebugLogger;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles drag and drop of ingredients from JEI to folders.
 */
public class DragDropHandler implements IGhostIngredientHandler<Screen> {
    
    /**
     * Creates a new drag drop handler for JEI ghost ingredients.
     */
    public DragDropHandler() {
        // Default constructor
    }
    
    /**
     * Gets the targets that can receive the dragged ingredient.
     *
     * @param gui The screen being displayed
     * @param ingredient The ingredient being dragged
     * @param doStart Whether this is the start of the drag operation
     * @return A list of possible targets for the dragged ingredient
     */
    @Nonnull
    public List<Target<?>> getTargets(@Nonnull Screen gui, @Nonnull Object ingredient, boolean doStart) {
        List<Target<?>> targets = new ArrayList<>();
        
        // only handle screens that implement target interface
        if (!(gui instanceof FolderGhostIngredientTarget targetGui)) {
            DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION,
                "Screen does not implement FolderGhostIngredientTarget: {}", gui.getClass().getName());
            return targets;
        }
        
        DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
            "DragDropHandler getting targets for GUI: {}, ingredient type: {}, doStart: {}", 
            gui.getClass().getSimpleName(), 
            ingredient.getClass().getSimpleName(), 
            doStart);
        
        // Get JEI integration
        Optional<JEIIntegration> jeiIntegration = DependencyProvider.get(JEIIntegration.class);
        if (jeiIntegration.isEmpty()) {
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEI integration not available");
            return targets;
        }
        
        // If doStart is true, JEI drag operation started
        if (doStart) {
            jeiIntegration.get().setCurrentDraggedObject(ingredient);
            DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                "Setting current dragged ingredient: {}", ingredient.getClass().getSimpleName());
        }
        
        Rect2i entireFolderArea = targetGui.getEntireFolderArea();
        DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
            "Adding entire folder screen area target: {}x{} at {}x{}", 
            entireFolderArea.getWidth(), entireFolderArea.getHeight(), 
            entireFolderArea.getX(), entireFolderArea.getY());
        
        // Create target for the entire folder area - this will accept drops to any active folder
        targets.add(FolderTargetFactory.createEntireFolderAreaTarget(entireFolderArea, targetGui));
        
        // Get targets for all visible folder buttons
        List<FolderButtonTarget> folderTargets = targetGui.getFolderTargets();
        DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, "Found folder button targets: {}", folderTargets.size());
        
        // Create targets for each folder button
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
            
            // Create target
            targets.add(FolderTargetFactory.createFolderButtonTarget(buttonArea, targetFolder, targetGui));
        }
        
        DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, "Total targets returned: {}", targets.size());
        return targets;
    }
    
    @Override
    @Nonnull
    public <I> List<Target<I>> getTargetsTyped(@Nonnull Screen gui, @Nonnull ITypedIngredient<I> ingredient, boolean doStart) {
        // Convert the result from other method to the appropriate type
        DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
            "getTargetsTyped called for: {}, ingredient type: {}, doStart: {}", 
            gui.getClass().getSimpleName(), 
            ingredient.getIngredient().getClass().getSimpleName(),
            doStart);
        
        @SuppressWarnings("unchecked")
        List<Target<I>> typedTargets = (List<Target<I>>) (List<?>) getTargets(gui, ingredient.getIngredient(), doStart);
        return typedTargets;
    }

    @Override
    public void onComplete() {
        DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "DragDropHandler.onComplete called");
        
        // Clear the current dragged ingredient reference
        DependencyProvider.get(JEIIntegration.class)
            .ifPresent(JEIIntegration::clearCurrentDraggedObject);
    }
    
    /**
     * This tells JEI whether targets should be highlighted
     * 
     * @return true to enable target highlighting
     */
    @Override
    public boolean shouldHighlightTargets() {
        DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "DragDropHandler.shouldHighlightTargets called - returning TRUE");
        return true;
    }
}
