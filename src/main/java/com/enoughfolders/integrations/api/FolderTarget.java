package com.enoughfolders.integrations.api;

import com.enoughfolders.data.Folder;

/**
 * A minimal interface for folder targets used after drag functionality removal.
 * This interface only provides basic folder access without drag functionality.
 */
public interface FolderTarget {
    /**
     * Gets the folder associated with this target.
     *
     * @return The folder
     */
    Folder getFolder();
}
