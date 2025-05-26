package com.enoughfolders.integrations.api;

import com.enoughfolders.data.Folder;

/**
 * A stub implementation of FolderTarget used after drag and drop removal.
 * This class provides only basic folder access without any drag functionality.
 */
public class FolderTargetStub implements FolderTarget {
    private final Folder folder;
    
    /**
     * Creates a new stub folder target.
     * 
     * @param folder The folder associated with this target
     */
    public FolderTargetStub(Folder folder) {
        this.folder = folder;
    }
    
    @Override
    public Folder getFolder() {
        return folder;
    }
}
