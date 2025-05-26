package com.enoughfolders.integrations.jei.gui.handlers;

import com.enoughfolders.client.event.ClientEventHandler;
import com.enoughfolders.integrations.common.handlers.BaseFolderScreenHandler;
import com.enoughfolders.util.DebugLogger;

import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Handler for JEI interactions with folder screens.
 */
public class FolderScreenHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {

    /**
     * Base handler that provides the common exclusion area logic
     */
    private final BaseFolderScreenHandler<Object, Object> baseHandler;

    /**
     * Creates a new folder screen handler for JEI integration.
     */
    public FolderScreenHandler() {
        // Create base handler with JEI-specific configuration
        this.baseHandler = new JEIBaseFolderHandler();
    }

    @Override
    @Nonnull
    public List<Rect2i> getGuiExtraAreas(@Nonnull AbstractContainerScreen<?> screen) {
        // Delegate to base handler for consistent exclusion area logic
        return baseHandler.getGuiExtraAreas(screen);
    }

    /**
     * JEI-specific implementation of the base folder screen handler.
     */
    private static class JEIBaseFolderHandler extends BaseFolderScreenHandler<Object, Object> {

        public JEIBaseFolderHandler() {
            super(ClientEventHandler::getFolderScreen, "JEI");
        }

        @Override
        @Nonnull
        public List<Object> getTargetsTyped(@Nonnull AbstractContainerScreen<?> gui, @Nonnull Object ingredient, boolean doStart) {
            // JEI doesn't use ghost ingredients through this handler
            return List.of();
        }

        @Override
        protected void onIntegrationComplete() {
            // No specific cleanup needed for JEI exclusion zones
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEI exclusion zone handling complete");
        }

        @Override
        protected void handleDragStart(Object ingredient) {
            // No drag tracking needed for exclusion zones
            DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                "JEI drag started with ingredient: {}", ingredient.getClass().getSimpleName());
        }

        @Override
        protected List<Object> createAllTargetsForScreen(com.enoughfolders.client.gui.FolderScreen folderScreen) {
            // No targets needed for exclusion zone handler
            return List.of();
        }
    }
}