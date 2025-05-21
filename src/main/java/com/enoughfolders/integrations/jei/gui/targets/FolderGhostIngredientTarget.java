package com.enoughfolders.integrations.jei.gui.targets;

import net.minecraft.client.renderer.Rect2i;

import java.util.Collections;
import java.util.List;

/**
 * Interface for screens that support JEI ghost ingredient drops onto folders.
 */
public interface FolderGhostIngredientTarget {

    /**
     * Get the rectangular area where ingredients can be dropped
     * into the active folder (grid area).
     * 
     * @return The content drop area
     */
    Rect2i getContentDropArea();
    
    /**
     * Get a list of all folder button targets that can receive
     * JEI ingredient drops directly. 
     * 
     * @return List of folder button targets, may be empty
     * @deprecated Use the RecipeViewingIntegration.createFolderTargets() method instead
     */
    @Deprecated
    default List<FolderButtonTarget> getFolderButtonTargets() {
        return Collections.emptyList();
    }
    
    /**
     * Called when an ingredient is successfully added to a folder.
     */
    void onIngredientAdded();
}
