package com.enoughfolders.integrations.api;

import com.enoughfolders.data.Folder;

/**
 * A common interface for representing folder targets across different integrations.
 */
public interface FolderTarget {
    /**
     * Checks if a point is within this target's bounds.
     *
     * @param x The X coordinate to check
     * @param y The Y coordinate to check
     * @return True if the point is within the target bounds
     */
    boolean isPointInTarget(double x, double y);
    
    /**
     * Gets the folder associated with this target.
     *
     * @return The folder
     */
    Folder getFolder();
    
    /**
     * Gets the X position of the target.
     *
     * @return The X position
     */
    int getX();
    
    /**
     * Gets the Y position of the target.
     *
     * @return The Y position
     */
    int getY();
    
    /**
     * Gets the width of the target.
     *
     * @return The width
     */
    int getWidth();
    
    /**
     * Gets the height of the target.
     *
     * @return The height
     */
    int getHeight();
}
