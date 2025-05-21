package com.enoughfolders.integrations.jei.gui.handlers;

import com.enoughfolders.client.event.ClientEventHandler;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.jei.core.JEIIntegrationCore;
import com.enoughfolders.integrations.jei.gui.targets.FolderButtonTarget;
import com.enoughfolders.integrations.jei.gui.targets.FolderGhostIngredientTarget;
import com.enoughfolders.integrations.jei.gui.targets.FolderTargetFactory;
import com.enoughfolders.util.DebugLogger;

import java.util.Collections;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles JEI integration for any container screen containing FolderScreen overlay.
 */
public class FolderScreenHandler implements 
    IGuiContainerHandler<AbstractContainerScreen<?>>,
    IGhostIngredientHandler<AbstractContainerScreen<?>>, 
    FolderGhostIngredientTarget {

    // Reference to the current container screen being handled
    private AbstractContainerScreen<?> currentScreen;

    @Override
    @Nonnull
    public List<Rect2i> getGuiExtraAreas(@Nonnull AbstractContainerScreen<?> screen) {
        List<Rect2i> areas = new ArrayList<>();
        
        // Store reference to the current screen
        currentScreen = screen;
        
        // Add the folder screen area if present
        ClientEventHandler.getFolderScreen(screen).ifPresent(folderScreen -> {
            // Get the folder screen area
            Rect2i screenArea = folderScreen.getScreenArea();
            
            // Add the area with a slight buffer to ensure JEI doesn't overlap
            areas.add(new Rect2i(
                screenArea.getX() - 2,
                screenArea.getY() - 2,
                screenArea.getWidth() + 4,
                screenArea.getHeight() + 4
            ));
            
            DebugLogger.debugValues(
                DebugLogger.Category.JEI_INTEGRATION,
                "Registered JEI excluded area: x={}, y={}, w={}, h={}",
                screenArea.getX() - 2, screenArea.getY() - 2,
                screenArea.getWidth() + 4, screenArea.getHeight() + 4
            );
        });
        
        return areas;
    }

    @Override
    @Nonnull
    public <I> List<Target<I>> getTargetsTyped(@Nonnull AbstractContainerScreen<?> gui, @Nonnull ITypedIngredient<I> ingredient, boolean doStart) {
        // Store reference to the current screen
        currentScreen = gui;
        
        DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
            "FolderScreenHandler getting targets for GUI: {}, ingredient type: {}, doStart: {}", 
            gui.getClass().getSimpleName(), 
            ingredient.getIngredient().getClass().getSimpleName(), 
            doStart);
        
        // Check for JEI integration
        Optional<JEIIntegrationCore> jeiIntegration = IntegrationRegistry.getIntegration(JEIIntegrationCore.class);
        if (jeiIntegration.isEmpty()) {
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEI integration not available");
            return new ArrayList<>();
        }
        
        // If doStart is true, JEI says a drag operation has started
        if (doStart) {
            jeiIntegration.get().setCurrentDraggedObject(ingredient.getIngredient());
            DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                "Setting current dragged ingredient: {}", 
                ingredient.getIngredient().getClass().getSimpleName());
        }
        
        // Create targets using the factory
        List<Target<I>> targets = new ArrayList<>();
        
        ClientEventHandler.getFolderScreen(gui).ifPresent(folderScreen -> {
            // Create all targets for this folder screen
            targets.addAll(FolderTargetFactory.createAllTargets(folderScreen, this));
        });
        
        DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
            "Total targets returned: {}", targets.size());
        return targets;
    }

    @Override
    public void onComplete() {
        DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "FolderScreenHandler.onComplete called");
        
        // Clear the current dragged ingredient reference
        IntegrationRegistry.getIntegration(JEIIntegrationCore.class)
            .ifPresent(JEIIntegrationCore::clearCurrentDraggedObject);
    }

    /**
     * Override shouldHighlightTargets to ensure drop targets are highlighted.
     * 
     * @return true to enable target highlighting
     */
    @Override
    public boolean shouldHighlightTargets() {
        DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, 
            "FolderScreenHandler: shouldHighlightTargets called, returning true");
        return true;
    }
    
    // Implementation of FolderGhostIngredientTarget interface
    
    @Override
    public Rect2i getContentDropArea() {
        if (currentScreen == null) {
            return new Rect2i(0, 0, 0, 0);
        }
        
        Optional<FolderScreen> folderScreenOpt = ClientEventHandler.getFolderScreen(currentScreen);
        if (folderScreenOpt.isPresent()) {
            return folderScreenOpt.get().getContentDropArea();
        }
        return new Rect2i(0, 0, 0, 0);
    }
    
    @Override
    public List<FolderButtonTarget> getFolderTargets() {
        if (currentScreen == null) {
            return Collections.emptyList();
        }
        
        Optional<FolderScreen> folderScreenOpt = ClientEventHandler.getFolderScreen(currentScreen);
        if (folderScreenOpt.isPresent()) {
            List<FolderButtonTarget> targets = folderScreenOpt.get().getFolderTargets();
            DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION,
                "FolderScreenHandler returning {} folder targets", targets.size());
            return targets;
        }
        return Collections.emptyList();
    }
        
    @Override
    public void onIngredientAdded() {
        // Clear the dragged ingredient from the JEI integration
        IntegrationRegistry.getIntegration(JEIIntegrationCore.class)
            .ifPresent(JEIIntegrationCore::clearCurrentDraggedObject);
        
        if (currentScreen == null) {
            return;
        }
        
        Optional<FolderScreen> folderScreenOpt = ClientEventHandler.getFolderScreen(currentScreen);
        folderScreenOpt.ifPresent(FolderScreen::onIngredientAdded);
    }
}