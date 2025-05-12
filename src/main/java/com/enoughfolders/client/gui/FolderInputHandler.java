package com.enoughfolders.client.gui;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Handles input events for the folder screen.
 */
public class FolderInputHandler {
    // Screen position and dimensions
    private int leftPos;
    private int topPos;
    private int width;
    private int height;
    
    // Callbacks for input events
    private final Consumer<String> createFolderCallback;
    private final Runnable toggleAddFolderModeCallback;
    
    /**
     * Creates a new folder input handler.
     *
     * @param createFolderCallback Callback for folder creation
     * @param toggleAddFolderModeCallback Callback for toggling add folder mode
     */
    public FolderInputHandler(
            Consumer<String> createFolderCallback,
            Runnable toggleAddFolderModeCallback) {
        this.createFolderCallback = createFolderCallback;
        this.toggleAddFolderModeCallback = toggleAddFolderModeCallback;
    }
    
    /**
     * Sets the position and dimensions.
     *
     * @param leftPos Left position
     * @param topPos Top position
     * @param width Width of the container
     * @param height Height of the container
     */
    public void setPositionAndDimensions(int leftPos, int topPos, int width, int height) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.width = width;
        this.height = height;
    }
    
    /**
     * Updates the height dimension.
     *
     * @param height New height
     */
    public void updateHeight(int height) {
        this.height = height;
    }
    
    /**
     * Checks if a point is within the folder screen's bounds.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @return true if the point is within the folder screen, false otherwise
     */
    public boolean isVisible(double mouseX, double mouseY) {
        return mouseX >= leftPos && mouseX < leftPos + width &&
               mouseY >= topPos && mouseY < topPos + height;
    }
    
    /**
     * Handles mouse click events on the folder screen.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param button The mouse button that was clicked
     * @param folderButtons The folder buttons
     * @param ingredientSlots The ingredient slots
     * @param addFolderButton The add folder button
     * @param deleteButton The delete button
     * @param prevPageButton The previous page button
     * @param nextPageButton The next page button
     * @param newFolderNameInput The new folder name input field
     * @param isAddingFolder Whether we're currently adding a folder
     * @param hasActiveFolder Whether there's an active folder
     * @return true if the click was handled, false otherwise
     */
    public boolean mouseClicked(
            double mouseX, 
            double mouseY, 
            int button,
            List<FolderButton> folderButtons,
            List<IngredientSlot> ingredientSlots,
            Button addFolderButton,
            Button deleteButton,
            Button prevPageButton,
            Button nextPageButton,
            EditBox newFolderNameInput,
            boolean isAddingFolder,
            boolean hasActiveFolder) {
            
        DebugLogger.debugValues(DebugLogger.Category.MOUSE, 
            "FolderInputHandler.mouseClicked at x:{}, y:{}, button:{}", 
            mouseX, mouseY, button);
        
        if (!isVisible(mouseX, mouseY)) {
            DebugLogger.debug(DebugLogger.Category.MOUSE, "Click outside folder screen area, ignoring");
            return false;
        }
        
        boolean overAddButton = addFolderButton.isMouseOver(mouseX, mouseY);
        DebugLogger.debugValues(DebugLogger.Category.MOUSE, 
            "Mouse over add folder button: {} at position {},{} with size {}x{}", 
            overAddButton, 
            addFolderButton.getX(), 
            addFolderButton.getY(),
            addFolderButton.getWidth(),
            addFolderButton.getHeight());
        
        if (overAddButton) {
            DebugLogger.debug(DebugLogger.Category.INPUT, "Add folder button clicked!");
            addFolderButton.onClick(mouseX, mouseY);
            return true;
        }
        
        for (FolderButton folderButton : folderButtons) {
            if (folderButton.isPointInButton((int) mouseX, (int) mouseY)) {
                folderButton.onClick();
                return true;
            }
        }
        
        for (IngredientSlot slot : ingredientSlots) {
            if (slot.mouseClicked((int) mouseX, (int) mouseY, button)) {
                return true;
            }
        }
        
        if (isAddingFolder && newFolderNameInput.isMouseOver(mouseX, mouseY)) {
            newFolderNameInput.setFocused(true);
            return newFolderNameInput.mouseClicked(mouseX, mouseY, button);
        }
        
        if (hasActiveFolder && deleteButton.isMouseOver(mouseX, mouseY)) {
            deleteButton.onClick(mouseX, mouseY);
            return true;
        }
        
        if (hasActiveFolder) {
            if (prevPageButton.isMouseOver(mouseX, mouseY)) {
                prevPageButton.onClick(mouseX, mouseY);
                return true;
            }
            
            if (nextPageButton.isMouseOver(mouseX, mouseY)) {
                nextPageButton.onClick(mouseX, mouseY);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Handles mouse release events on the folder screen.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param button The mouse button that was released
     * @param folderButtons The folder buttons
     * @param onIngredientAdded Callback for when an ingredient is added
     * @return true if the release was handled, false otherwise
     */
    public boolean mouseReleased(
            double mouseX, 
            double mouseY, 
            int button,
            List<FolderButton> folderButtons,
            Runnable onIngredientAdded) {
            
        EnoughFolders.LOGGER.info("FolderInputHandler.mouseReleased at {},{}, button: {}", mouseX, mouseY, button);
        
        if (!isVisible(mouseX, mouseY)) {
            return false;
        }
        
        for (FolderButton folderButton : folderButtons) {
            if (folderButton.tryHandleDrop((int)mouseX, (int)mouseY)) {
                EnoughFolders.LOGGER.info("Mouse release handled as drop on folder button");
                onIngredientAdded.run();
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Handles keyboard key press events.
     *
     * @param keyCode The key code
     * @param scanCode The scan code
     * @param modifiers The modifier keys
     * @param isAddingFolder Whether we're currently adding a folder
     * @param newFolderNameInput The new folder name input field
     * @return true if the key press was handled, false otherwise
     */
    public boolean keyPressed(
            int keyCode, 
            int scanCode, 
            int modifiers,
            boolean isAddingFolder,
            EditBox newFolderNameInput) {
            
        if (isAddingFolder && newFolderNameInput.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                createFolderCallback.accept(newFolderNameInput.getValue());
                newFolderNameInput.setValue("");
                toggleAddFolderModeCallback.run();
                return true;
            }
            
            return newFolderNameInput.keyPressed(keyCode, scanCode, modifiers);
        }
        
        return false;
    }
    
    /**
     * Handles character input events.
     *
     * @param codePoint The character code point
     * @param modifiers The modifier keys
     * @param isAddingFolder Whether we're currently adding a folder
     * @param newFolderNameInput The new folder name input field
     * @return true if the character input was handled, false otherwise
     */
    public boolean charTyped(
            char codePoint, 
            int modifiers,
            boolean isAddingFolder,
            EditBox newFolderNameInput) {
            
        if (isAddingFolder && newFolderNameInput.isFocused()) {
            return newFolderNameInput.charTyped(codePoint, modifiers);
        }
        
        return false;
    }
}
