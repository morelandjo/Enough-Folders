package com.enoughfolders.client.data;

import com.enoughfolders.client.gui.FolderButtonManager;
import com.enoughfolders.client.gui.IngredientGridManager;
import com.enoughfolders.data.FolderManager;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Folder screen rendering object.
 */
public class RenderContext {
    private final AbstractContainerScreen<?> parentScreen;
    private final FolderManager folderManager;
    private final FolderButtonManager buttonManager;
    private final IngredientGridManager gridManager;
    
    // Screen position and dimensions
    private int leftPos;
    private int topPos;
    private int width;
    private int height;
    
    /**
     * Creates a new render context.
     *
     * @param parentScreen The container screen that hosts the folder UI
     * @param folderManager The folder manager that provides folder data
     * @param buttonManager The button manager that handles folder buttons
     * @param gridManager The grid manager that handles ingredient slots
     */
    public RenderContext(
            AbstractContainerScreen<?> parentScreen,
            FolderManager folderManager,
            FolderButtonManager buttonManager,
            IngredientGridManager gridManager) {
        this.parentScreen = parentScreen;
        this.folderManager = folderManager;
        this.buttonManager = buttonManager;
        this.gridManager = gridManager;
    }
    
    /**
     * Sets the position and dimensions for rendering.
     *
     * @param leftPos Left position
     * @param topPos Top position
     * @param width Width of the container
     * @param height Height of the container
     */
    public void setPositionAndDimensions(int leftPos, int topPos, int width, int height) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.width = width;
        this.height = height;
    }
    
    /**
     * Updates the height dimension.
     *
     * @param height New height
     */
    public void updateHeight(int height) {
        this.height = height;
    }
    
    /**
     * Gets the parent container screen.
     * 
     * @return The parent screen
     */
    public AbstractContainerScreen<?> getParentScreen() {
        return parentScreen;
    }
    
    /**
     * Gets the folder manager.
     * 
     * @return The folder manager
     */
    public FolderManager getFolderManager() {
        return folderManager;
    }
    
    /**
     * Gets the button manager.
     * 
     * @return The button manager
     */
    public FolderButtonManager getButtonManager() {
        return buttonManager;
    }
    
    /**
     * Gets the ingredient grid manager.
     * 
     * @return The ingredient grid manager
     */
    public IngredientGridManager getGridManager() {
        return gridManager;
    }
    
    /**
     * Gets the left position.
     * 
     * @return The left position
     */
    public int getLeftPos() {
        return leftPos;
    }
    
    /**
     * Gets the top position.
     * 
     * @return The top position
     */
    public int getTopPos() {
        return topPos;
    }
    
    /**
     * Gets the width.
     * 
     * @return The width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the height.
     * 
     * @return The height
     */
    public int getHeight() {
        return height;
    }
}
