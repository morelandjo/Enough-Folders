package com.enoughfolders.integrations.rei.gui.targets;

import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.data.Folder;
import com.enoughfolders.integrations.api.FolderTargetFactory;

/**
 * Implementation of FolderTargetFactory for REI folder targets.
 */
public class REIFolderTargetFactory implements FolderTargetFactory<REIFolderTarget> {
    
    /**
     * Singleton instance.
     */
    private static final REIFolderTargetFactory INSTANCE = new REIFolderTargetFactory();
    
    /**
     * Gets the singleton instance.
     * 
     * @return The singleton factory instance
     */
    public static REIFolderTargetFactory getInstance() {
        return INSTANCE;
    }
    
    /**
     * Private constructor for singleton pattern.
     */
    private REIFolderTargetFactory() {}
    
    /**
     * Creates a REI folder target from a folder button.
     * 
     * @param button The folder button to create a target for
     * @return The created REI folder target
     */
    @Override
    public REIFolderTarget createTarget(FolderButton button) {
        return createTarget(
            button.getX() - 1,
            button.getY() - 1,
            button.getWidth() + 2,
            button.getHeight() + 2,
            button.getFolder()
        );
    }
    
    /**
     * Creates a REI folder target from coordinates and a folder.
     * 
     * @param x The X coordinate of the target
     * @param y The Y coordinate of the target
     * @param width The width of the target
     * @param height The height of the target
     * @param folder The folder associated with the target
     * @return The created REI folder target
     */
    @Override
    public REIFolderTarget createTarget(int x, int y, int width, int height, Folder folder) {
        return new REIFolderTarget(x, y, width, height, folder);
    }
}
