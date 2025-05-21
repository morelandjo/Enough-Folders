package com.enoughfolders.integrations.api;

import java.util.List;

import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.data.Folder;

/**
 * Factory interface for creating folder targets for different mod integrations.
 * 
 * @param <T> The specific type of folder target this factory creates
 */
public interface FolderTargetFactory<T extends FolderTarget> {
    
    /**
     * Creates a folder target from a folder button.
     * 
     * @param button The folder button to create a target for
     * @return The created folder target
     */
    T createTarget(FolderButton button);
    
    /**
     * Creates a folder target from coordinates and a folder.
     * 
     * @param x The X coordinate of the target
     * @param y The Y coordinate of the target
     * @param width The width of the target
     * @param height The height of the target
     * @param folder The folder associated with the target
     * @return The created folder target
     */
    T createTarget(int x, int y, int width, int height, Folder folder);
    
    /**
     * Creates a list of targets from a list of folder buttons.
     * 
     * @param buttons The list of folder buttons
     * @return A list of folder targets
     */
    default List<T> createTargets(List<FolderButton> buttons) {
        return buttons.stream()
            .map(this::createTarget)
            .toList();
    }
}
