package com.enoughfolders.client.gui;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Manages the ingredient grid in the folder screen.
 */
public class IngredientGridManager {
    private static final int CONTENT_SLOT_SIZE = 18;
    private static final int INGREDIENT_SPACING = 0;
    private static final int INGREDIENT_ROWS = 4;
    private static final int FOLDER_AREA_HEIGHT = 22;
    
    // List of all ingredient slots in the active folder view
    private final List<IngredientSlot> ingredientSlots = new ArrayList<>();
    
    // Pagination
    private Button prevPageButton;
    private Button nextPageButton;
    private int currentPage = 0;
    private int totalPages = 1;
    
    // Screen properties
    private int leftPos;
    private int topPos;
    private int width;
    private int height;
    private int ingredientColumns = 5;
    
    // Dependencies
    private final Supplier<Optional<Folder>> activeFolder;
    
    /**
     * Creates a new ingredient grid manager.
     *
     * @param activeFolderSupplier Supplier for the active folder
     */
    public IngredientGridManager(Supplier<Optional<Folder>> activeFolderSupplier) {
        this.activeFolder = activeFolderSupplier;
    }
    
    /**
     * Sets the position and dimensions for layout calculations.
     *
     * @param leftPos Left position
     * @param topPos Top position
     * @param width Width of the grid
     */
    public void setPositionAndDimensions(int leftPos, int topPos, int width) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.width = width;
        calculateIngredientColumns();
    }

    /**
     * Creates pagination buttons.
     *
     * @param prevPageCallback Callback when previous page button is clicked
     * @param nextPageCallback Callback when next page button is clicked
     */
    public void createPaginationButtons(Button.OnPress prevPageCallback, Button.OnPress nextPageCallback) {
        this.prevPageButton = new Button.Builder(
                net.minecraft.network.chat.Component.literal("<"), 
                prevPageCallback)
                .pos(leftPos + 5, topPos + FOLDER_AREA_HEIGHT + 32)
                .size(20, 20)
                .build();
        
        this.nextPageButton = new Button.Builder(
                net.minecraft.network.chat.Component.literal(">"), 
                nextPageCallback)
                .pos(leftPos + width - 25, topPos + FOLDER_AREA_HEIGHT + 32)
                .size(20, 20)
                .build();
                
        updatePagination();
    }
    
    /**
     * Calculates the number of ingredient columns based on available width.
     */
    private void calculateIngredientColumns() {
        int availableWidth = width - 10;
        int ingredientWidth = CONTENT_SLOT_SIZE + INGREDIENT_SPACING;
        int maxColumns = Math.max(1, (availableWidth - 1) / ingredientWidth);
        this.ingredientColumns = maxColumns;
        DebugLogger.debugValues(DebugLogger.Category.GUI_STATE, 
                              "Dynamically calculated ingredient columns: {} (from available width: {}px)", 
                              ingredientColumns, availableWidth);
    }
    
    /**
     * Refreshes the ingredient slots for the active folder.
     *
     * @param isAddingFolder Whether we're currently in add folder mode
     * @param folderRowsCount Number of folder button rows
     * @return The new container height
     */
    public int refreshIngredientSlots(boolean isAddingFolder, int folderRowsCount) {
        ingredientSlots.clear();
        
        return activeFolder.get().map(folder -> {
            List<StoredIngredient> ingredients = folder.getIngredients();
            
            int itemsPerPage = ingredientColumns * INGREDIENT_ROWS;
            totalPages = Math.max(1, (int) Math.ceil(ingredients.size() / (double) itemsPerPage));
            
            if (currentPage >= totalPages) {
                currentPage = totalPages - 1;
            }
            
            int startIndex = currentPage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, ingredients.size());
            
            int verticalOffset = isAddingFolder ? 20 : 0; // INPUT_FIELD_HEIGHT
            
            if (folderRowsCount > 1) {
                verticalOffset += (folderRowsCount - 1) * 27; // FOLDER_ROW_HEIGHT
            }
            
            int paginationY = topPos + FOLDER_AREA_HEIGHT + 32 + verticalOffset;
            prevPageButton.setPosition(leftPos + 5, paginationY);
            nextPageButton.setPosition(leftPos + width - 25, paginationY);
            
            int contentStartX = leftPos + 5;
            int contentStartY = topPos + FOLDER_AREA_HEIGHT + 55 + verticalOffset;
            
            int totalSlotsOnPage = endIndex - startIndex;
            int rowsUsed = (int) Math.ceil(totalSlotsOnPage / (double) ingredientColumns);
            
            int rowsNeeded = Math.max(rowsUsed + 1, 1);
            
            int contentHeight = rowsNeeded * CONTENT_SLOT_SIZE;
            
            int newTotalHeight = FOLDER_AREA_HEIGHT + 55 + contentHeight + 20 + verticalOffset;
            
            height = newTotalHeight;
            
            DebugLogger.debugValues(DebugLogger.Category.GUI_STATE, 
                              "Ingredients grid layout: {} columns x {} rows (including 1 extra row), totalItems: {}",
                              ingredientColumns, rowsNeeded, totalSlotsOnPage);
            
            DebugLogger.debugValues(DebugLogger.Category.GUI_STATE, 
                "Resizing folder screen to fit {} rows (including 1 extra row), new height: {}, vertical offset: {}", 
                rowsNeeded, height, verticalOffset);
            
            for (int i = startIndex; i < endIndex; i++) {
                int slotIndex = i - startIndex;
                int row = slotIndex / ingredientColumns;
                int col = slotIndex % ingredientColumns;
                
                int x = contentStartX + col * CONTENT_SLOT_SIZE;
                int y = contentStartY + row * CONTENT_SLOT_SIZE;
                
                ingredientSlots.add(new IngredientSlot(x, y, ingredients.get(i)));
            }
            
            updatePagination();
            return height;
        }).orElse(FOLDER_AREA_HEIGHT + 10); // Default height when no active folder
    }
    
    /**
     * Gets the area where ingredients can be dropped in the active folder.
     *
     * @param isAddingFolder Whether we're currently in add folder mode
     * @param folderRowsCount Number of folder button rows
     * @return A rectangle representing the drop area
     */
    public Rect2i getContentDropArea(boolean isAddingFolder, int folderRowsCount) {
        if (!activeFolder.get().isPresent()) {
            return new Rect2i(leftPos + 5, topPos + FOLDER_AREA_HEIGHT + 55, 0, 0);
        }
        
        int itemsPerPage = ingredientColumns * INGREDIENT_ROWS;
        int startIndex = currentPage * itemsPerPage;
        
        List<StoredIngredient> ingredients = activeFolder.get().get().getIngredients();
        int endIndex = Math.min(startIndex + itemsPerPage, ingredients.size());
        int totalSlotsOnPage = endIndex - startIndex;
        
        int rowsUsed = (int) Math.ceil(totalSlotsOnPage / (double) ingredientColumns);
        
        // Always ensure at least 3 rows of drop area are available, or more if needed
        int rowsNeeded = Math.max(rowsUsed + 1, 3);
        
        int gridWidth = ingredientColumns * CONTENT_SLOT_SIZE;
        int gridHeight = rowsNeeded * CONTENT_SLOT_SIZE;
        
        // Log the calculated content drop area dimensions
        EnoughFolders.LOGGER.info("Calculated content drop area: rows={}, totalSlotsOnPage={}, gridWidth={}, gridHeight={}", 
            rowsNeeded, totalSlotsOnPage, gridWidth, gridHeight);
        
        int verticalOffset = isAddingFolder ? 20 : 0; // INPUT_FIELD_HEIGHT
        
        if (folderRowsCount > 1) {
            verticalOffset += (folderRowsCount - 1) * 27; // FOLDER_ROW_HEIGHT
        }
        
        return new Rect2i(
                leftPos + 5,
                topPos + FOLDER_AREA_HEIGHT + 55 + verticalOffset,
                gridWidth,
                gridHeight
        );
    }
    
    /**
     * Updates the pagination controls based on the current page and total pages.
     */
    private void updatePagination() {
        boolean hasMultiplePages = totalPages > 1;
        prevPageButton.active = hasMultiplePages;
        nextPageButton.active = hasMultiplePages;
        
        DebugLogger.debugValues(DebugLogger.Category.GUI_STATE, 
            "Pagination updated: currentPage={}, totalPages={}, buttons active={}", 
            currentPage, totalPages, hasMultiplePages);
    }
    
    /**
     * Navigates to the previous page of ingredients.
     */
    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
        } else {
            currentPage = totalPages - 1;
        }
    }
    
    /**
     * Navigates to the next page of ingredients.
     */
    public void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
        } else {
            currentPage = 0;
        }
    }
    
    /**
     * Gets all ingredient slots.
     *
     * @return List of ingredient slots
     */
    public List<IngredientSlot> getIngredientSlots() {
        return ingredientSlots;
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
     * Gets the current page number (0-based).
     * 
     * @return The current page
     */
    public int getCurrentPage() {
        return currentPage;
    }
    
    /**
     * Gets the total number of pages.
     * 
     * @return The total pages
     */
    public int getTotalPages() {
        return totalPages;
    }
    
    /**
     * Gets the number of ingredient columns.
     * 
     * @return The number of ingredient columns
     */
    public int getIngredientColumns() {
        return ingredientColumns;
    }
}
