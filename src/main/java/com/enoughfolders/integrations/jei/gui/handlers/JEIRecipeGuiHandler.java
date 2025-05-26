package com.enoughfolders.integrations.jei.gui.handlers;

import com.enoughfolders.integrations.common.handlers.BaseRecipeGuiHandler;
import com.enoughfolders.util.DebugLogger;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles JEI recipe GUI interactions for container screens.
 * 
 * @param <T> The type of container screen this handler works with
 */
public class JEIRecipeGuiHandler<T extends AbstractContainerScreen<?>> 
        extends BaseRecipeGuiHandler<T> implements IGuiContainerHandler<T> {
    
    /**
     * Creates a new JEI recipe GUI handler.
     */
    public JEIRecipeGuiHandler() {
        // Default constructor
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
    public List<Rect2i> getGuiExtraAreas(@Nonnull T screen) {
        return new ArrayList<>();
    }
}