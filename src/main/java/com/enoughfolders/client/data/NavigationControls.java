package com.enoughfolders.client.data;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;

/**
 * Data object containing the UI control elements for navigation in the folder screen.
 */
public class NavigationControls {
    private final Button addFolderButton;
    private final Button deleteButton;
    private final Button prevPageButton;
    private final Button nextPageButton;
    private final EditBox newFolderNameInput;
    
    /**
     * Creates a new navigation controls object.
     * 
     * @param addFolderButton The button for adding a new folder
     * @param deleteButton The button for deleting the current folder
     * @param prevPageButton The button for navigating to the previous page
     * @param nextPageButton The button for navigating to the next page
     * @param newFolderNameInput The input field for entering a new folder name
     */
    public NavigationControls(
            Button addFolderButton, 
            Button deleteButton, 
            Button prevPageButton, 
            Button nextPageButton, 
            EditBox newFolderNameInput) {
        this.addFolderButton = addFolderButton;
        this.deleteButton = deleteButton;
        this.prevPageButton = prevPageButton;
        this.nextPageButton = nextPageButton;
        this.newFolderNameInput = newFolderNameInput;
    }

    /**
     * Gets the add folder button.
     * 
     * @return The add folder button
     */
    public Button getAddFolderButton() {
        return addFolderButton;
    }

    /**
     * Gets the delete button.
     * 
     * @return The delete button
     */
    public Button getDeleteButton() {
        return deleteButton;
    }

    /**
     * Gets the previous page button.
     * 
     * @return The previous page button
     */
    public Button getPrevPageButton() {
        return prevPageButton;
    }

    /**
     * Gets the next page button.
     * 
     * @return The next page button
     */
    public Button getNextPageButton() {
        return nextPageButton;
    }

    /**
     * Gets the new folder name input field.
     * 
     * @return The new folder name input field
     */
    public EditBox getNewFolderNameInput() {
        return newFolderNameInput;
    }
}
