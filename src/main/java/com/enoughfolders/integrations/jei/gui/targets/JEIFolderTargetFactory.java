package com.enoughfolders.integrations.jei.gui.targets;

import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.data.Folder;
import com.enoughfolders.integrations.api.FolderTargetFactory;

/**
 * Implementation of FolderTargetFactory for JEI folder targets.
 */
public class JEIFolderTargetFactory implements FolderTargetFactory<FolderButtonTarget> {
    
    /**
     * Singleton instance.
     */
    private static final JEIFolderTargetFactory INSTANCE = new JEIFolderTargetFactory();
    
    /**
     * Gets the singleton instance.
     * 
     * @return The singleton factory instance
     */
    public static JEIFolderTargetFactory getInstance() {
        return INSTANCE;
    }
    
    /**
     * Private constructor for singleton pattern.
     */
    private JEIFolderTargetFactory() {}
    
    /**
     * Creates a JEI folder target from a folder button.
     * 
     * @param button The folder button to create a target for
     * @return The created JEI folder target
     */
    @Override
    public FolderButtonTarget createTarget(FolderButton button) {
        return createTarget(
            button.getX() - 1,
            button.getY() - 1,
            button.getWidth() + 2,
            button.getHeight() + 2,
            button.getFolder()
        );
    }
    
    /**
     * Creates a JEI folder target from coordinates and a folder.
     * 
     * @param x The X coordinate of the target
     * @param y The Y coordinate of the target
     * @param width The width of the target
     * @param height The height of the target
     * @param folder The folder associated with the target
     * @return The created JEI folder target
     */
    @Override
    public FolderButtonTarget createTarget(int x, int y, int width, int height, Folder folder) {
        return new FolderButtonTarget(x, y, width, height, folder);
    }
}
