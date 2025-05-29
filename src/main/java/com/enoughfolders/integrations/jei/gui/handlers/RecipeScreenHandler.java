package com.enoughfolders.integrations.jei.gui.handlers;

import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.integrations.common.handlers.BaseRecipeGuiHandler;
import com.enoughfolders.util.DebugLogger;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Global handler for JEI recipe screens.
 */
public class RecipeScreenHandler extends BaseRecipeGuiHandler<Screen> implements IGlobalGuiHandler {
    
    // Cache the exclusion areas to avoid recalculating them frequently
    private static Collection<Rect2i> cachedAreas = new ArrayList<>();
    
    /**
     * Creates a new recipe screen handler for managing folder UI on recipe screens.
     */
    public RecipeScreenHandler() {
        DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "RecipeScreenHandler created for JEI recipe exclusion areas");
    }
    
    /**
     * Check if the given screen is a valid JEI recipe screen.
     * 
     * @param screen The screen to check
     * @return true if it's a JEI recipe screen
     */
    protected static boolean isValidRecipeScreen(Screen screen) {
        return screen instanceof IRecipesGui;
    }
    
    /**
     * Get the integration name for JEI.
     * 
     * @return The integration name
     */
    protected static String getIntegrationName() {
        return "JEI";
    }
    
    /**
     * Get the debug category for JEI logging.
     * 
     * @return The debug category
     */
    protected static DebugLogger.Category getDebugCategory() {
        return DebugLogger.Category.JEI_INTEGRATION;
    }

    @Override
    @Nonnull
    public Collection<Rect2i> getGuiExtraAreas() {
        Collection<Rect2i> areas = new ArrayList<>();
        
        Screen currentScreen = Minecraft.getInstance().screen;
        
        DebugLogger.debugValues(getDebugCategory(), 
            "Current screen type for {} exclusion areas: {}", 
            getIntegrationName(),
            currentScreen != null ? currentScreen.getClass().getName() : "null");
            
        if (isValidRecipeScreen(currentScreen)) {
            Optional<FolderScreen> folderScreenOpt = getLastFolderScreen();
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
        // This method is for providing additional clickable ingredients beyond JEI's normal detection.
        // Since we don't have any custom ingredients in recipe screens, we return empty to let
        // JEI handle its normal ingredient detection without interference.
        
        DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION, 
            "RecipeScreenHandler.getClickableIngredientUnderMouse called at ({}, {}) - returning empty to allow normal JEI detection", 
            mouseX, mouseY);
        
        return Optional.empty();
    }
}