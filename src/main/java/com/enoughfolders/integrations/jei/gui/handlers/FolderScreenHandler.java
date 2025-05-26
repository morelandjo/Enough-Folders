package com.enoughfolders.integrations.jei.gui.handlers;

import com.enoughfolders.client.event.ClientEventHandler;
import com.enoughfolders.util.DebugLogger;

import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler for JEI interactions with folder screens.
 * Handles GUI area exclusions to prevent JEI overlay conflicts.
 */
public class FolderScreenHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {

    /**
     * Creates a new folder screen handler for JEI integration.
     */
    public FolderScreenHandler() {
        // Default constructor
    }

    @Override
    @Nonnull
    public List<Rect2i> getGuiExtraAreas(@Nonnull AbstractContainerScreen<?> screen) {
        List<Rect2i> areas = new ArrayList<>();
        
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
}