package com.enoughfolders.integrations.common.handlers;

import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Base handler for folder screen GUI integration.
 * 
 * @param <T> The specific GUI handler interface type for the integration
 * @param <I> The ingredient type for the integration
 */
public abstract class BaseFolderScreenHandler<T, I> {

    /**
     * Reference to the current container screen being handled
     */
    protected AbstractContainerScreen<?> currentScreen;
    
    // Integration-specific screen retrieval function
    private final Function<AbstractContainerScreen<?>, Optional<FolderScreen>> folderScreenRetriever;
    
    // Integration name for logging
    private final String integrationName;

    /**
     * Constructor for base folder screen handler.
     * 
     * @param folderScreenRetriever Function to retrieve folder screen from container screen
     * @param integrationName Name of the integration for logging purposes
     */
    protected BaseFolderScreenHandler(
            Function<AbstractContainerScreen<?>, Optional<FolderScreen>> folderScreenRetriever,
            String integrationName) {
        this.folderScreenRetriever = folderScreenRetriever;
        this.integrationName = integrationName;
    }

    /**
     * Gets GUI extra areas (exclusion zones) for the integration.
     * 
     * @param screen The container screen to check for folder overlays
     * @return List of rectangles representing areas to exclude from integration rendering
     */
    @Nonnull
    public List<Rect2i> getGuiExtraAreas(@Nonnull AbstractContainerScreen<?> screen) {
        List<Rect2i> areas = new ArrayList<>();
        
        // Store reference to the current screen
        currentScreen = screen;
        
        // Add the folder screen area if present
        folderScreenRetriever.apply(screen).ifPresent(folderScreen -> {
            // Get the folder screen area
            Rect2i screenArea = folderScreen.getScreenArea();
            
            // Add the area with a slight buffer to ensure integration doesn't overlap
            areas.add(new Rect2i(
                screenArea.getX() - 2,
                screenArea.getY() - 2,
                screenArea.getWidth() + 4,
                screenArea.getHeight() + 4
            ));
            
            DebugLogger.debugValues(
                DebugLogger.Category.INTEGRATION,
                "Registered {} excluded area: x={}, y={}, w={}, h={}",
                integrationName,
                screenArea.getX() - 2, screenArea.getY() - 2,
                screenArea.getWidth() + 4, screenArea.getHeight() + 4
            );
        });
        
        return areas;
    }

    /**
     * Gets targets for ghost ingredient handling.
     * 
     * @param gui The GUI screen
     * @param ingredient The typed ingredient being dragged
     * @param doStart Whether this is the start of a drag operation
     * @return List of drop targets for the ingredient
     */
    @Nonnull
    public abstract List<T> getTargetsTyped(@Nonnull AbstractContainerScreen<?> gui, @Nonnull I ingredient, boolean doStart);

    /**
     * Called when a ghost ingredient drag operation completes.
     */
    public void onComplete() {
        DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
            "{} handler onComplete called", integrationName);
        onIntegrationComplete();
    }

    /**
     * Template method for integration-specific cleanup operations.
     */
    protected abstract void onIntegrationComplete();

    /**
     * Determines whether drop targets should be highlighted during drag operations.
     * 
     * @return true to enable target highlighting, false otherwise
     */
    public boolean shouldHighlightTargets() {
        DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
            "{} handler: shouldHighlightTargets called, returning true", integrationName);
        return true;
    }

    /**
     * Template method for handling drag start operations.
     * 
     * @param ingredient The ingredient being dragged
     */
    protected abstract void handleDragStart(Object ingredient);

    /**
     * Creates all targets for a folder screen using integration-specific factory methods.
     * 
     * @param folderScreen The folder screen to create targets for
     * @return List of targets for the folder screen
     */
    protected abstract List<T> createAllTargetsForScreen(FolderScreen folderScreen);

    /**
     * Gets the current folder screen from the stored screen reference.
     * 
     * @return Optional containing the folder screen if available
     */
    protected Optional<FolderScreen> getCurrentFolderScreen() {
        if (currentScreen == null) {
            return Optional.empty();
        }
        return folderScreenRetriever.apply(currentScreen);
    }

    /**
     * Common implementation for getting targets with integration-specific handling.
     * 
     * @param gui The GUI screen
     * @param ingredient The ingredient being dragged
     * @param doStart Whether this is the start of a drag operation
     * @return List of targets
     */
    protected List<T> getTargetsCommon(@Nonnull AbstractContainerScreen<?> gui, @Nonnull Object ingredient, boolean doStart) {
        // Store reference to the current screen
        currentScreen = gui;
        
        DebugLogger.debugValues(DebugLogger.Category.INTEGRATION,
            "{} handler getting targets for GUI: {}, ingredient type: {}, doStart: {}", 
            integrationName,
            gui.getClass().getSimpleName(), 
            ingredient.getClass().getSimpleName(), 
            doStart);

        // If doStart is true, the integration says a drag operation has started
        if (doStart) {
            handleDragStart(ingredient);
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Setting current dragged ingredient: {}", 
                ingredient.getClass().getSimpleName());
        }

        // Create targets using the integration-specific factory
        List<T> targets = new ArrayList<>();
        
        getCurrentFolderScreen().ifPresent(folderScreen -> {
            // Create all targets for this folder screen
            targets.addAll(createAllTargetsForScreen(folderScreen));
        });

        DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
            "Total targets returned: {}", targets.size());
        return targets;
    }

    /**
     * Gets the integration name for logging purposes.
     * 
     * @return The integration name
     */
    protected String getIntegrationName() {
        return integrationName;
    }
}
