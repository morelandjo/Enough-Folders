package com.enoughfolders.integrations.emi.gui.targets;

import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.data.Folder;
import com.enoughfolders.integrations.api.FolderTarget;
import com.enoughfolders.integrations.emi.core.EMIIntegration;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.util.DebugLogger;

/**
 * Represents a folder as a target for EMI drag and drop operations.
 */
public class EMIFolderTarget implements FolderTarget {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final Folder folder;
    
    /**
     * Creates a new EMI folder target from a folder screen.
     */
    public EMIFolderTarget(FolderScreen folderScreen) {
        // Use the getScreenArea method to get position and size information
        net.minecraft.client.renderer.Rect2i screenArea = folderScreen.getScreenArea();
        this.x = screenArea.getX();
        this.y = screenArea.getY();
        this.width = screenArea.getWidth();
        this.height = screenArea.getHeight();
        
        // Get the active folder from the FolderManager
        this.folder = com.enoughfolders.EnoughFolders.getInstance().getFolderManager().getActiveFolder()
                .orElse(null);
        
        DebugLogger.debugValue(
            DebugLogger.Category.INTEGRATION,
            "Created EMI folder target for folder: {}", 
            folder != null ? folder.getName() : "null"
        );
    }
    
    /**
     * Creates a new EMI folder target.
     * 
     * @param x X position of the button
     * @param y Y position of the button
     * @param width Width of the button
     * @param height Height of the button
     * @param folder The folder this button represents
     */
    public EMIFolderTarget(int x, int y, int width, int height, Folder folder) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.folder = folder;
    }
    
    /**
     * Handle a drop operation on this folder target.
     */
    public boolean handleDrop() {
        try {
            if (folder == null) {
                return false;
            }
            
            // Use EMI integration directly instead of EMIDragManager
            return IntegrationRegistry.getIntegration(EMIIntegration.class)
                .map(integration -> integration.handleIngredientDrop(folder))
                .orElse(false);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error handling drop in EMI folder target: {}", 
                e.getMessage()
            );
            return false;
        }
    }
    
    /**
     * Check if this target can accept the current drag.
     */
    public boolean canAcceptDrag() {
        try {
            // Check if EMI integration has a dragged ingredient and folder is available
            return IntegrationRegistry.getIntegration(EMIIntegration.class)
                .map(integration -> integration.getDraggedIngredient().isPresent() && folder != null)
                .orElse(false);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error checking drag acceptance in EMI folder target: {}", 
                e.getMessage()
            );
            return false;
        }
    }
    
    /**
     * Checks if the given point is within this target.
     * 
     * @param x X coordinate to check
     * @param y Y coordinate to check
     * @return True if the point is within this target
     */
    @Override
    public boolean isPointInTarget(double x, double y) {
        return x >= this.x && x <= this.x + this.width &&
               y >= this.y && y <= this.y + this.height;
    }
    
    /**
     * Gets the folder associated with this target.
     * 
     * @return The folder
     */
    @Override
    public Folder getFolder() {
        return folder;
    }
    
    /**
     * Gets the X position of the target.
     * 
     * @return The X position
     */
    @Override
    public int getX() {
        return x;
    }
    
    /**
     * Gets the Y position of the target.
     * 
     * @return The Y position
     */
    @Override
    public int getY() {
        return y;
    }
    
    /**
     * Gets the width of the target.
     * 
     * @return The width
     */
    @Override
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the height of the target.
     * 
     * @return The height
     */
    @Override
    public int getHeight() {
        return height;
    }
}
