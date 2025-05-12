package com.enoughfolders.integrations.jei.gui.targets;

import com.enoughfolders.data.Folder;
import net.minecraft.client.renderer.Rect2i;

/**
 * Represents a folder button that can be a target for JEI ingredient drops.
 */
public class FolderButtonTarget {
    private final Rect2i area;
    private final Folder folder;
    
    /**
     * Create a new folder button target
     * @param x X position of the button
     * @param y Y position of the button
     * @param width Width of the button
     * @param height Height of the button
     * @param folder The folder this button represents
     */
    public FolderButtonTarget(int x, int y, int width, int height, Folder folder) {
        this.area = new Rect2i(x, y, width, height);
        this.folder = folder;
    }
    
    /**
     * Get the area of the folder button
     * @return The area rectangle
     */
    public Rect2i getArea() {
        return area;
    }
    
    /**
     * Get the folder this button represents
     * @return The folder
     */
    public Folder getFolder() {
        return folder;
    }
}