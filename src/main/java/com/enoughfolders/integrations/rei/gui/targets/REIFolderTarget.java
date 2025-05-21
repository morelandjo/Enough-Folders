package com.enoughfolders.integrations.rei.gui.targets;

import com.enoughfolders.data.Folder;
import com.enoughfolders.integrations.api.FolderTarget;
import me.shedaniel.math.Rectangle;

/**
 * Represents a folder button that can be a target for REI ingredient drops.
 */
public class REIFolderTarget implements FolderTarget {
    private final Rectangle area;
    private final Folder folder;
    
    /**
     * Creates a new REI folder target.
     * 
     * @param x X position of the button
     * @param y Y position of the button
     * @param width Width of the button
     * @param height Height of the button
     * @param folder The folder this button represents
     */
    public REIFolderTarget(int x, int y, int width, int height, Folder folder) {
        this.area = new Rectangle(x, y, width, height);
        this.folder = folder;
    }
    
    /**
     * Gets the area rectangle for REI operations.
     * 
     * @return The area as a REI Rectangle
     */
    public Rectangle getAreaRectangle() {
        return area;
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
        return area.contains(x, y);
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
        return area.getX();
    }
    
    /**
     * Gets the Y position of the target.
     * 
     * @return The Y position
     */
    @Override
    public int getY() {
        return area.getY();
    }
    
    /**
     * Gets the width of the target.
     * 
     * @return The width
     */
    @Override
    public int getWidth() {
        return area.getWidth();
    }
    
    /**
     * Gets the height of the target.
     * 
     * @return The height
     */
    @Override
    public int getHeight() {
        return area.getHeight();
    }
}
