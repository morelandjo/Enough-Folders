// Import UIConstants class
package com.enoughfolders.client.gui;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.jei.core.JEIIntegrationCore;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.renderer.Rect2i;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Centralized manager for layout calculations across the EnoughFolders UI.
 * Handles positioning and dimensions for all UI components.
 */
public class LayoutManager {
    
    // Position and dimensions
    private int leftPos;
    private int topPos;
    private int width;
    private int height;
    
    // State variables affecting layout
    private boolean isAddingFolder;
    private int folderRowsCount;
    private final Supplier<Optional<Folder>> activeFolderSupplier;
    
    // Layout change listeners
    private final List<LayoutChangeListener> listeners = new java.util.ArrayList<>();
    
    /**
     * Creates a new layout manager.
     *
     * @param activeFolderSupplier Supplier for the active folder
     */
    public LayoutManager(Supplier<Optional<Folder>> activeFolderSupplier) {
        this.activeFolderSupplier = activeFolderSupplier;
    }
    
    /**
     * Interface for classes that need to be notified of layout changes.
     */
    public interface LayoutChangeListener {
        /**
         * Called when the layout changes.
         */
        void onLayoutChanged();
    }
    
    /**
     * Adds a listener to be notified of layout changes.
     *
     * @param listener The listener to add
     */
    public void addLayoutChangeListener(LayoutChangeListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a layout change listener.
     *
     * @param listener The listener to remove
     */
    public void removeLayoutChangeListener(LayoutChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifies all listeners that the layout has changed.
     */
    private void notifyLayoutChanged() {
        for (LayoutChangeListener listener : listeners) {
            listener.onLayoutChanged();
        }
    }
    
    /**
     * Calculates the initial screen dimensions based on parent screen size and integrations.
     *
     * @param parentWidth The width of the parent screen
     * @param parentHeight The height of the parent screen
     */
    public void calculateInitialDimensions(int parentWidth, int parentHeight) {
        int parentLeftPos = 0;
        
        // Calculate the parent screen's left position for container screens
        int standardContainerWidth = 176;
        parentLeftPos = (parentWidth - standardContainerWidth) / 2;
        
        // Check if JEI recipe GUI is open
        boolean isJeiRecipeGuiOpen = false;
        Optional<JEIIntegrationCore> jeiIntegration = IntegrationRegistry.getIntegration(JEIIntegrationCore.class);
        if (jeiIntegration.isPresent()) {
            isJeiRecipeGuiOpen = jeiIntegration.get().isRecipeGuiOpen();
        }
        
        // Calculate maximum width based on screen size
        int maxWidth = Math.min(parentWidth - 40, 387);
        
        // Adjust width to avoid parent screen overlap
        if (parentLeftPos > 0) {
            int originalWidth = maxWidth;
            maxWidth = Math.min(maxWidth, parentLeftPos - 20);
            if (originalWidth != maxWidth) {
                DebugLogger.debugValues(DebugLogger.Category.GUI_STATE,
                    "Width limited to avoid parent screen overlap, from {} to {}", originalWidth, maxWidth);
            }
        }
        
        // Reduce width if JEI recipe GUI is open
        if (isJeiRecipeGuiOpen) {
            int originalWidth = maxWidth;
            maxWidth = Math.max(70, maxWidth - UIConstants.JEI_WIDTH_REDUCTION);
            DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION, 
                "Reduced width for JEI recipe GUI by {}, original: {}, new width: {}", 
                UIConstants.JEI_WIDTH_REDUCTION, originalWidth, maxWidth);
        }
        
        width = maxWidth;
        leftPos = 5;
        topPos = 5;
        
        // Check for FTB sidebar overlap and adjust position if necessary
        checkAndAdjustForFTBSidebar();
        
        notifyLayoutChanged();
    }
    
    /**
     * Checks if FTB Library is loaded and if the sidebar would overlap with folder GUI.
     * Adjusts the position if necessary.
     */
    private void checkAndAdjustForFTBSidebar() {
        // Check for FTB Library integration
        if (com.enoughfolders.integrations.ftb.FTBIntegration.isFTBLibraryLoaded()) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "Checking for FTB sidebar overlap");
            
            // Create a rectangle representing our current folder GUI position
            Rect2i folderRect = new Rect2i(leftPos, topPos, width, 100);
            
            // Ask FTB integration to adjust the position if needed
            Rect2i adjustedRect = com.enoughfolders.integrations.ftb.FTBIntegration.avoidExclusionAreas(folderRect);
            
            // If position was adjusted, update our position
            if (adjustedRect.getY() != topPos) {
                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION,
                    "FTB sidebar detected, adjusted Y position from {} to {}", 
                    topPos, adjustedRect.getY());
                
                topPos = adjustedRect.getY();
                notifyLayoutChanged();
            }
        }
    }
    
    /**
     * Updates height based on content and notifies listeners.
     *
     * @param height The new height
     */
    public void updateHeight(int height) {
        if (this.height != height) {
            this.height = height;
            notifyLayoutChanged();
        }
    }
    
    /**
     * Updates the folder rows count and recalculates layout.
     *
     * @param folderRowsCount The new folder rows count
     */
    public void setFolderRowsCount(int folderRowsCount) {
        if (this.folderRowsCount != folderRowsCount) {
            this.folderRowsCount = folderRowsCount;
            recalculateHeightIfNeeded();
        }
    }
    
    /**
     * Updates the adding folder state and recalculates layout.
     *
     * @param isAddingFolder The new adding folder state
     */
    public void setIsAddingFolder(boolean isAddingFolder) {
        if (this.isAddingFolder != isAddingFolder) {
            this.isAddingFolder = isAddingFolder;
            recalculateHeightIfNeeded();
        }
    }
    
    /**
     * Recalculates height if necessary based on state changes.
     */
    private void recalculateHeightIfNeeded() {
        // Basic height calculation that will be refined by component managers
        int newHeight = UIConstants.FOLDER_AREA_HEIGHT;
        
        // Add height for folder rows
        if (folderRowsCount > 1) {
            newHeight += (folderRowsCount - 1) * UIConstants.FOLDER_ROW_HEIGHT;
        }
        
        // Add height if a folder is active
        if (activeFolderSupplier.get().isPresent()) {
            newHeight += 97; // Base height for active folder content
        } else {
            newHeight += 10; // Minimal padding when no folder is active
        }
        
        // Add input field height if adding folder
        if (isAddingFolder) {
            newHeight += UIConstants.INPUT_FIELD_HEIGHT;
        }
        
        updateHeight(newHeight);
    }
    
    /**
     * Gets the position for the add folder button.
     *
     * @return X and Y coordinates for the add folder button
     */
    public int[] getAddFolderButtonPosition() {
        return new int[] { leftPos + 5, topPos + 5 };
    }
    
    /**
     * Gets the position for the delete button.
     *
     * @return X and Y coordinates for the delete button
     */
    public int[] getDeleteButtonPosition() {
        return new int[] { 
            leftPos + width - 25, 
            topPos + UIConstants.FOLDER_AREA_HEIGHT + 5 
        };
    }
    
    /**
     * Gets the position for the folder name input field.
     *
     * @return X, Y, and width for the folder name input
     */
    public int[] getFolderNameInputPosition() {
        return new int[] { 
            leftPos + 30, 
            topPos + 7, 
            width - 35 
        };
    }
    
    /**
     * Calculates the start position and dimensions for folder buttons.
     *
     * @param isAddingFolder Whether we're currently in add folder mode
     * @return Array with startX, currentY, availableWidth, and firstRowWidth
     */
    public int[] calculateFolderButtonLayout(boolean isAddingFolder) {
        int startX = leftPos + 5 + UIConstants.FOLDER_WIDTH + 5;
        int currentY = topPos + 5;
        
        // Adjust Y position if adding folder
        if (isAddingFolder) {
            currentY += UIConstants.INPUT_FIELD_HEIGHT;
        }
        
        int availableWidth = width - 10;
        int firstRowWidth = availableWidth - (UIConstants.FOLDER_WIDTH + 5);
        
        return new int[] { startX, currentY, availableWidth, firstRowWidth };
    }
    
    /**
     * Calculates positions for pagination buttons.
     *
     * @param isAddingFolder Whether we're currently in add folder mode
     * @return X, Y coordinates for previous and next page buttons
     */
    public int[] getPaginationButtonPositions(boolean isAddingFolder) {
        int verticalOffset = isAddingFolder ? UIConstants.INPUT_FIELD_HEIGHT : 0;
        
        if (folderRowsCount > 1) {
            verticalOffset += (folderRowsCount - 1) * UIConstants.FOLDER_ROW_HEIGHT;
        }
        
        int paginationY = topPos + UIConstants.FOLDER_AREA_HEIGHT + 32 + verticalOffset;
        
        return new int[] {
            leftPos + 5, // prevButton X
            paginationY, // prevButton Y
            leftPos + width - 25, // nextButton X
            paginationY  // nextButton Y
        };
    }
    
    /**
     * Calculates the content area position and dimensions.
     *
     * @param isAddingFolder Whether we're currently in add folder mode
     * @param ingredientColumns Number of ingredient columns
     * @param ingredients List of ingredients to display
     * @param currentPage Current page number
     * @return Content area info: startX, startY, rowsNeeded, verticalOffset
     */
    public int[] calculateContentArea(boolean isAddingFolder, int ingredientColumns, 
                                   List<StoredIngredient> ingredients, int currentPage) {
        int verticalOffset = isAddingFolder ? UIConstants.INPUT_FIELD_HEIGHT : 0;
        
        if (folderRowsCount > 1) {
            verticalOffset += (folderRowsCount - 1) * UIConstants.FOLDER_ROW_HEIGHT;
        }
        
        int contentStartX = leftPos + 5;
        int contentStartY = topPos + UIConstants.FOLDER_AREA_HEIGHT + 55 + verticalOffset;
        
        // Calculate items per page and pagination
        int itemsPerPage = ingredientColumns * UIConstants.INGREDIENT_ROWS;
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, ingredients.size());
        int totalSlotsOnPage = endIndex - startIndex;
        
        // Calculate rows needed
        int rowsUsed = (int) Math.ceil(totalSlotsOnPage / (double) ingredientColumns);
        int rowsNeeded = Math.max(rowsUsed + 1, 1);
        
        return new int[] { contentStartX, contentStartY, rowsNeeded, verticalOffset };
    }
    
    /**
     * Gets the content drop area for ingredient dropping.
     *
     * @param isAddingFolder Whether we're currently in add folder mode
     * @param ingredientColumns Number of ingredient columns
     * @return Rectangle representing the drop area
     */
    public Rect2i getContentDropArea(boolean isAddingFolder, int ingredientColumns) {
        // Only calculate if there is an active folder
        if (!activeFolderSupplier.get().isPresent()) {
            return new Rect2i(leftPos + 5, topPos + UIConstants.FOLDER_AREA_HEIGHT + 55, 0, 0);
        }
        
        List<StoredIngredient> ingredients = activeFolderSupplier.get().get().getIngredients();
        
        int verticalOffset = isAddingFolder ? UIConstants.INPUT_FIELD_HEIGHT : 0;
        
        if (folderRowsCount > 1) {
            verticalOffset += (folderRowsCount - 1) * UIConstants.FOLDER_ROW_HEIGHT;
        }
        
        // Always ensure at least 3 rows of drop area are available
        int rowsNeeded = Math.max(3, 1);
        
        int gridWidth = ingredientColumns * UIConstants.INGREDIENT_SLOT_SIZE;
        int gridHeight = rowsNeeded * UIConstants.INGREDIENT_SLOT_SIZE;
        
        return new Rect2i(
                leftPos + 5,
                topPos + UIConstants.FOLDER_AREA_HEIGHT + 55 + verticalOffset,
                gridWidth,
                gridHeight
        );
    }
    
    /**
     * Checks if a point is within the folder screen's bounds.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @return true if the point is within the folder screen, false otherwise
     */
    public boolean isPointInside(double mouseX, double mouseY) {
        return mouseX >= leftPos && mouseX < leftPos + width &&
               mouseY >= topPos && mouseY < topPos + height;
    }
    
    /**
     * Gets the current left position.
     *
     * @return The left position
     */
    public int getLeftPos() {
        return leftPos;
    }
    
    /**
     * Gets the current top position.
     *
     * @return The top position
     */
    public int getTopPos() {
        return topPos;
    }
    
    /**
     * Gets the current width.
     *
     * @return The width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the current height.
     *
     * @return The height
     */
    public int getHeight() {
        return height;
    }
}
