// Import UIConstants class
package com.enoughfolders.client.gui;

import com.enoughfolders.util.DebugLogger;
import com.enoughfolders.client.data.FolderContentState;
import com.enoughfolders.client.data.NavigationControls;
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
    
    // Deprecated mouseClicked method removed - use mouseClicked(double, double, int, FolderContentState, NavigationControls, boolean) instead
    
    /**
     * Handles mouse click events on the folder screen using data objects.
     * This is the preferred method to use for new code.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param button The mouse button that was clicked
     * @param contentState The folder content state
     * @param controls The navigation controls
     * @param hasActiveFolder Whether there's an active folder
     * @return true if the click was handled, false otherwise
     */
    public boolean mouseClicked(
            double mouseX, 
            double mouseY, 
            int button,
            FolderContentState contentState,
            NavigationControls controls,
            boolean hasActiveFolder) {
            
        DebugLogger.debugValues(DebugLogger.Category.MOUSE, 
            "FolderInputHandler.mouseClicked at x:{}, y:{}, button:{}", 
            mouseX, mouseY, button);
        
        if (!isVisible(mouseX, mouseY)) {
            DebugLogger.debug(DebugLogger.Category.MOUSE, "Click outside folder screen area, ignoring");
            return false;
        }
        
        Button addFolderButton = controls.getAddFolderButton();
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
        
        List<FolderButton> folderButtons = contentState.getFolderButtons();
        for (FolderButton folderButton : folderButtons) {
            if (folderButton.isPointInButton((int) mouseX, (int) mouseY)) {
                folderButton.onClick();
                return true;
            }
        }
        
        EditBox newFolderNameInput = controls.getNewFolderNameInput();
        if (contentState.isAddingFolder() && newFolderNameInput.isMouseOver(mouseX, mouseY)) {
            newFolderNameInput.setFocused(true);
            return newFolderNameInput.mouseClicked(mouseX, mouseY, button);
        }
        
        Button deleteButton = controls.getDeleteButton();
        if (hasActiveFolder && deleteButton.isMouseOver(mouseX, mouseY)) {
            deleteButton.onClick(mouseX, mouseY);
            return true;
        }
        
        if (hasActiveFolder) {
            Button prevPageButton = controls.getPrevPageButton();
            Button nextPageButton = controls.getNextPageButton();
            
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
    
    // Deprecated mouseReleased method removed - use mouseReleased(double, double, int, FolderContentState, Runnable, FolderScreen) instead
    
    /**
     * Handles mouse release events on the folder screen using data objects.
     * This is the preferred method to use for new code.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param button The mouse button that was released
     * @param contentState The folder content state 
     * @param onIngredientAdded Callback for when an ingredient is added
     * @param folderScreen The folder screen to use for integration handling
     * @return true if the release was handled, false otherwise
     */
    public boolean mouseReleased(
            double mouseX, 
            double mouseY, 
            int button,
            FolderContentState contentState,
            Runnable onIngredientAdded,
            FolderScreen folderScreen) {
            
        DebugLogger.debugValues(DebugLogger.Category.MOUSE, 
            "FolderInputHandler.mouseReleased at x:{}, y:{}, button:{}", 
            mouseX, mouseY, button);
        
        if (!isVisible(mouseX, mouseY)) {
            DebugLogger.debug(DebugLogger.Category.MOUSE, "Release outside folder screen area, ignoring");
            return false;
        }
        
        // Handle ingredient drag and drop via integrations
        if (folderScreen != null) {
            if (folderScreen.getIntegrationHandler().handleIngredientDrop(
                    mouseX, mouseY, contentState.getFolderButtons())) {
                onIngredientAdded.run();
                return true;
            }
        }
        
        return false;
    }
    
    // Deprecated keyPressed method removed - use keyPressed(int, int, int, FolderContentState, NavigationControls) instead
    
    /**
     * Handles keyboard key press events using data objects.
     * This is the preferred method to use for new code.
     *
     * @param keyCode The key code
     * @param scanCode The scan code
     * @param modifiers The modifier keys
     * @param contentState The folder content state
     * @param controls The navigation controls
     * @return true if the key press was handled, false otherwise
     */
    public boolean keyPressed(
            int keyCode, 
            int scanCode, 
            int modifiers,
            FolderContentState contentState,
            NavigationControls controls) {
            
        EditBox newFolderNameInput = controls.getNewFolderNameInput();
        
        if (contentState.isAddingFolder() && newFolderNameInput.isFocused()) {
            if (keyCode == 257 || keyCode == 335) { // Enter or numpad enter
                createFolderCallback.accept(newFolderNameInput.getValue());
                newFolderNameInput.setValue("");
                toggleAddFolderModeCallback.run();
                return true;
            }
            
            return newFolderNameInput.keyPressed(keyCode, scanCode, modifiers);
        }
        
        return false;
    }
    
    // Deprecated charTyped method removed - use charTyped(char, int, FolderContentState, NavigationControls) instead
    
    /**
     * Handles character input events using data objects.
     * This is the preferred method to use for new code.
     *
     * @param codePoint The character code point
     * @param modifiers The modifier keys
     * @param contentState The folder content state
     * @param controls The navigation controls
     * @return true if the character input was handled, false otherwise
     */
    public boolean charTyped(
            char codePoint, 
            int modifiers,
            FolderContentState contentState,
            NavigationControls controls) {
            
        EditBox newFolderNameInput = controls.getNewFolderNameInput();
        
        if (contentState.isAddingFolder() && newFolderNameInput.isFocused()) {
            return newFolderNameInput.charTyped(codePoint, modifiers);
        }
        
        return false;
    }
    
    /**
     * Process a click on an ingredient slot.
     * 
     * @param folderScreen The folder screen that contains the slots
     * @param slot The slot that was clicked
     * @param mouseX The mouse X position
     * @param mouseY The mouse Y position 
     * @param button The mouse button used
     * @return true if the click was handled by any handler, false otherwise
     */
    public boolean processIngredientClick(FolderScreen folderScreen, IngredientSlot slot, double mouseX, double mouseY, int button) {
        if (slot.isHovered((int)mouseX, (int)mouseY)) {
            // Get modifier keys
            boolean shiftHeld = net.minecraft.client.gui.screens.Screen.hasShiftDown();
            boolean ctrlHeld = net.minecraft.client.gui.screens.Screen.hasControlDown();
            
            // Try the integration handler first
            if (folderScreen.getIntegrationHandler().handleIngredientClick(slot, button, shiftHeld, ctrlHeld)) {
                return true;
            }
            
            // Try registered handlers next
            if (folderScreen.notifyIngredientClickHandlers(slot, button, shiftHeld, ctrlHeld)) {
                return true;
            }
            
            // Fall back to default behavior if no handler processed it
            return slot.mouseClicked((int)mouseX, (int)mouseY, button);
        }
        return false;
    }
}
