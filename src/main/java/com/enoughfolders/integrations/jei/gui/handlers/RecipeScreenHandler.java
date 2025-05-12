package com.enoughfolders.integrations.jei.gui.handlers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.integrations.jei.gui.targets.FolderButtonTarget;
import com.enoughfolders.integrations.jei.gui.targets.FolderGhostIngredientTarget;
import com.enoughfolders.integrations.jei.gui.targets.FolderTargetFactory;
import com.enoughfolders.util.DebugLogger;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Global handler for JEI recipe screens.
 */
public class RecipeScreenHandler implements IGlobalGuiHandler, IGhostIngredientHandler<Screen>, FolderGhostIngredientTarget {
    
    // Whether screen type on this frame is logged
    private static Screen lastLoggedScreen = null;
    // Cache the exclusion areas to avoid recalculating them frequently
    private static Collection<Rect2i> cachedAreas = new ArrayList<>();
    // Time of last diagnostic log to prevent excessive logging
    private static long lastLogTime = 0;
    // Log frequency limiter (milliseconds)
    private static final long LOG_INTERVAL = 1000; // Log at most once per second
    
    public RecipeScreenHandler() {
        EnoughFolders.LOGGER.debug("RecipeScreenHandler created for JEI recipe exclusion areas");
    }

    @Override
    @Nonnull
    public Collection<Rect2i> getGuiExtraAreas() {
        Collection<Rect2i> areas = new ArrayList<>();
        
        Screen currentScreen = Minecraft.getInstance().screen;
        
        // Only log screen type when it changes to reduce spam
        long currentTime = System.currentTimeMillis();
        if (lastLoggedScreen != currentScreen && (currentTime - lastLogTime > LOG_INTERVAL)) {
            lastLoggedScreen = currentScreen;
            lastLogTime = currentTime;
            DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION, 
                "Current screen type for JEI exclusion areas: {}", 
                currentScreen != null ? currentScreen.getClass().getName() : "null");
        }
            
        if (currentScreen instanceof IRecipesGui) {
            Optional<FolderScreen> folderScreenOpt = JEIRecipeGuiHandler.getLastFolderScreen();
            if (folderScreenOpt.isPresent()) {
                FolderScreen folderScreen = folderScreenOpt.get();
                Rect2i screenArea = folderScreen.getScreenArea();
                
                // Add the area with a slight buffer to ensure JEI doesn't overlap
                areas.add(new Rect2i(
                    screenArea.getX() - 2,
                    screenArea.getY() - 2,
                    screenArea.getWidth() + 4,
                    screenArea.getHeight() + 4
                ));
                
                // Cache the areas
                cachedAreas = areas;
                return areas;
            }
        }
        
        // Return cached areas if no new ones were generated
        return !cachedAreas.isEmpty() ? cachedAreas : areas;
    }
    
    @Override
    public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(double mouseX, double mouseY) {
        //no clickable ingredients
        return Optional.empty();
    }
    
    /**
     * Implementation of IGhostIngredientHandler methods
     */
    @Override
    @Nonnull
    public <I> List<Target<I>> getTargetsTyped(@Nonnull Screen gui, @Nonnull ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        
        if (!(gui instanceof IRecipesGui)) {
            return targets;
        }
        
        DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
            "RecipeScreenHandler getting targets for GUI: {}, ingredient type: {}, doStart: {}", 
            gui.getClass().getSimpleName(), 
            ingredient.getIngredient().getClass().getSimpleName(), 
            doStart);
        
        // Get the folder screen
        Optional<FolderScreen> folderScreenOpt = JEIRecipeGuiHandler.getLastFolderScreen();
        if (folderScreenOpt.isEmpty()) {
            return targets;
        }
        
        FolderScreen folderScreen = folderScreenOpt.get();
        
        // Use the factory to create all targets for this folder screen
        targets.addAll(FolderTargetFactory.createAllTargets(folderScreen, this));
        
        DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
            "Total targets returned: {}", targets.size());
        return targets;
    }
    
    @Override
    public void onComplete() {
        DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "RecipeScreenHandler.onComplete called");
    }
    
    @Override
    public boolean shouldHighlightTargets() {
        return true;
    }
    
    // Implementation of FolderGhostIngredientTarget interface
    @Override
    public Rect2i getContentDropArea() {
        Optional<FolderScreen> folderScreenOpt = JEIRecipeGuiHandler.getLastFolderScreen();
        if (folderScreenOpt.isPresent()) {
            FolderScreen folderScreen = folderScreenOpt.get();
            return folderScreen.getContentDropArea();
        }
        return new Rect2i(0, 0, 0, 0); // Empty area if no folder screen
    }
    
    @Override
    public List<FolderButtonTarget> getFolderButtonTargets() {
        Optional<FolderScreen> folderScreenOpt = JEIRecipeGuiHandler.getLastFolderScreen();
        if (folderScreenOpt.isPresent()) {
            FolderScreen folderScreen = folderScreenOpt.get();
            
            // Get the folder button targets from the folder screen
            List<FolderButtonTarget> targets = folderScreen.getFolderButtonTargets();
            DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                "RecipeScreenHandler returning {} folder button targets", targets.size());
            return targets;
        }
        return new ArrayList<>(); // Empty list if no folder screen
    }
    
    @Override
    public void onIngredientAdded() {
        // Notify the folder screen that an ingredient was added
        Optional<FolderScreen> folderScreenOpt = JEIRecipeGuiHandler.getLastFolderScreen();
        folderScreenOpt.ifPresent(FolderScreen::onIngredientAdded);
    }
}