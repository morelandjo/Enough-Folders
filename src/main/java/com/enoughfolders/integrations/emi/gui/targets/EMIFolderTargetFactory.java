package com.enoughfolders.integrations.emi.gui.targets;

import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.data.Folder;
import com.enoughfolders.integrations.api.FolderTargetFactory;
import com.enoughfolders.util.DebugLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of FolderTargetFactory for EMI folder targets.
 */
public class EMIFolderTargetFactory implements FolderTargetFactory<EMIFolderTarget> {
    
    /**
     * Singleton instance.
     */
    private static final EMIFolderTargetFactory INSTANCE = new EMIFolderTargetFactory();
    
    /**
     * Gets the singleton instance.
     * 
     * @return The singleton factory instance
     */
    public static EMIFolderTargetFactory getInstance() {
        return INSTANCE;
    }
    
    /**
     * Private constructor for singleton pattern.
     */
    private EMIFolderTargetFactory() {}
    
    /**
     * Creates a EMI folder target from a folder button.
     * 
     * @param button The folder button to create a target for
     * @return The created EMI folder target
     */
    @Override
    public EMIFolderTarget createTarget(FolderButton button) {
        return createTarget(
            button.getX() - 1,
            button.getY() - 1,
            button.getWidth() + 2,
            button.getHeight() + 2,
            button.getFolder()
        );
    }
    
    /**
     * Creates a EMI folder target from coordinates and a folder.
     * 
     * @param x The X coordinate of the target
     * @param y The Y coordinate of the target
     * @param width The width of the target
     * @param height The height of the target
     * @param folder The folder associated with the target
     * @return The created EMI folder target
     */
    @Override
    public EMIFolderTarget createTarget(int x, int y, int width, int height, Folder folder) {
        return new EMIFolderTarget(x, y, width, height, folder);
    }

    /**
     * Creates folder targets for a list of folder buttons.
     * 
     * @param folderButtons The list of folder buttons to create targets for
     * @return A list of EMI folder targets
     */
    public List<EMIFolderTarget> createTargets(List<FolderButton> folderButtons) {
        List<EMIFolderTarget> targets = new ArrayList<>();
        
        for (FolderButton button : folderButtons) {
            try {
                targets.add(createTarget(button));
                DebugLogger.debugValue(
                    DebugLogger.Category.INTEGRATION,
                    "Created EMI folder target for folder: {}",
                    button.getFolder().getName()
                );
            } catch (Exception e) {
                DebugLogger.debugValue(
                    DebugLogger.Category.INTEGRATION,
                    "Error creating EMI folder target: {}",
                    e.getMessage()
                );
            }
        }
        
        return targets;
    }
}
