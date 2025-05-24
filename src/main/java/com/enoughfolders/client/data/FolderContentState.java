package com.enoughfolders.client.data;

import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.client.gui.IngredientSlot;

import java.util.List;

/**
 * Data object containing the content state for the folder UI.
 */
public class FolderContentState {
    private final boolean isAddingFolder;
    private final int currentPage;
    private final int totalPages;
    private final int folderRowsCount;
    private final List<FolderButton> folderButtons;
    private final List<IngredientSlot> ingredientSlots;
    
    /**
     * Creates a new folder content state object.
     * 
     * @param isAddingFolder Whether a new folder is being added
     * @param currentPage The current page of ingredients (0-based)
     * @param totalPages The total number of pages
     * @param folderRowsCount The number of rows of folder buttons
     * @param folderButtons The list of folder buttons
     * @param ingredientSlots The list of ingredient slots in the current view
     */
    public FolderContentState(
            boolean isAddingFolder, 
            int currentPage, 
            int totalPages, 
            int folderRowsCount,
            List<FolderButton> folderButtons,
            List<IngredientSlot> ingredientSlots) {
        this.isAddingFolder = isAddingFolder;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.folderRowsCount = folderRowsCount;
        this.folderButtons = folderButtons;
        this.ingredientSlots = ingredientSlots;
    }

    /**
     * Checks if a new folder is being added.
     * 
     * @return True if a new folder is being added, false otherwise
     */
    public boolean isAddingFolder() {
        return isAddingFolder;
    }

    /**
     * Gets the current page of ingredients.
     * 
     * @return The current page
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Gets the total number of pages.
     * 
     * @return The total number of pages
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * Gets the number of rows of folder buttons.
     * 
     * @return The number of folder button rows
     */
    public int getFolderRowsCount() {
        return folderRowsCount;
    }

    /**
     * Gets the list of folder buttons.
     * 
     * @return The list of folder buttons
     */
    public List<FolderButton> getFolderButtons() {
        return folderButtons;
    }

    /**
     * Gets the list of ingredient slots in the current view.
     * 
     * @return The list of ingredient slots
     */
    public List<IngredientSlot> getIngredientSlots() {
        return ingredientSlots;
    }
}
