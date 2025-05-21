package com.enoughfolders.integrations.api;

import java.util.List;

/**
 * Interface for integrations that can provide folder targets.
 */
public interface FolderTargetProvider {
    
    /**
     * Gets the display name of the integration.
     * 
     * @return The integration display name
     */
    String getDisplayName();
    
    /**
     * Checks if this folder target provider is available in the current environment.
     * 
     * @return True if the provider is available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Gets folder targets for all buttons in a folder screen.
     * 
     * @param folderButtons The list of folder buttons to create targets for
     * @return A list of folder targets
     */
    List<? extends FolderTarget> getFolderTargets(List<com.enoughfolders.client.gui.FolderButton> folderButtons);
}
