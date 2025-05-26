package com.enoughfolders.client.gui;

import com.enoughfolders.data.Folder;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.gui.components.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Manages the ingredient grid in the folder screen.
 */
public class IngredientGridManager implements LayoutManager.LayoutChangeListener {
    
    // List of all ingredient slots in the active folder view
    private final List<IngredientSlot> ingredientSlots = new ArrayList<>();
    
    // Pagination
    private Button prevPageButton;
    private Button nextPageButton;
    private int currentPage = 0;
    private int totalPages = 1;
    
    // Screen properties
    private int ingredientColumns = 5;
    
    // Dependencies
    private final Supplier<Optional<Folder>> activeFolder;
    private final LayoutManager layoutManager;
    
    /**
     * Creates a new ingredient grid manager.
     *
     * @param activeFolderSupplier Supplier for the active folder
     * @param layoutManager Layout manager for positioning
     */
    public IngredientGridManager(Supplier<Optional<Folder>> activeFolderSupplier, LayoutManager layoutManager) {
        this.activeFolder = activeFolderSupplier;
        this.layoutManager = layoutManager;
        this.layoutManager.addLayoutChangeListener(this);
        calculateIngredientColumns();
    }
    
    /**
     * Called when the layout changes.
     */
    @Override
    public void onLayoutChanged() {
        calculateIngredientColumns();
        
        if (prevPageButton != null && nextPageButton != null) {
            updatePaginationButtonPositions();
        }
    }
    
    /**
     * Calculates the number of ingredient columns based on available width.
     */
    private void calculateIngredientColumns() {
        int availableWidth = layoutManager.getWidth() - 6;
        int ingredientWidth = UIConstants.INGREDIENT_SLOT_SIZE + UIConstants.INGREDIENT_SPACING;
        int maxColumns = Math.max(1, availableWidth / ingredientWidth);
        this.ingredientColumns = maxColumns;
        DebugLogger.debugValues(DebugLogger.Category.GUI_STATE, 
                              "Dynamically calculated ingredient columns: {} (from available width: {}px)", 
                              ingredientColumns, availableWidth);
    }
    
    /**
     * Creates pagination buttons.
     *
     * @param prevPageCallback Callback when previous page button is clicked
     * @param nextPageCallback Callback when next page button is clicked
     */
    public void createPaginationButtons(Button.OnPress prevPageCallback, Button.OnPress nextPageCallback) {
        int[] paginationPositions = layoutManager.getPaginationButtonPositions(false);
        
        this.prevPageButton = new Button.Builder(
                net.minecraft.network.chat.Component.literal("<"), 
                prevPageCallback)
                .pos(paginationPositions[0], paginationPositions[1])
                .size(UIConstants.PAGE_BUTTON_WIDTH, UIConstants.PAGE_BUTTON_HEIGHT)
                .build();
        
        this.nextPageButton = new Button.Builder(
                net.minecraft.network.chat.Component.literal(">"), 
                nextPageCallback)
                .pos(paginationPositions[2], paginationPositions[3])
                .size(UIConstants.PAGE_BUTTON_WIDTH, UIConstants.PAGE_BUTTON_HEIGHT)
                .build();
                
        updatePagination();
    }
    
    /**
     * Updates the pagination button positions.
     */
    private void updatePaginationButtonPositions() {
        boolean isAddingFolder = false;
        int[] paginationPositions = layoutManager.getPaginationButtonPositions(isAddingFolder);
        
        prevPageButton.setPosition(paginationPositions[0], paginationPositions[1]);
        nextPageButton.setPosition(paginationPositions[2], paginationPositions[3]);
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
        layoutManager.setIsAddingFolder(isAddingFolder);
        layoutManager.setFolderRowsCount(folderRowsCount);
        
        return activeFolder.get().map(folder -> {
            List<StoredIngredient> ingredients = folder.getIngredients();
            
            int itemsPerPage = ingredientColumns * UIConstants.INGREDIENT_ROWS;
            totalPages = Math.max(1, (int) Math.ceil(ingredients.size() / (double) itemsPerPage));
            
            if (currentPage >= totalPages) {
                currentPage = totalPages - 1;
            }
            
            int startIndex = currentPage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, ingredients.size());
            
            int[] paginationPositions = layoutManager.getPaginationButtonPositions(isAddingFolder);
            prevPageButton.setPosition(paginationPositions[0], paginationPositions[1]);
            nextPageButton.setPosition(paginationPositions[2], paginationPositions[3]);
            
            int[] contentArea = layoutManager.calculateContentArea(
                    isAddingFolder, ingredientColumns, ingredients, currentPage);
            int contentStartX = contentArea[0];
            int contentStartY = contentArea[1];
            int rowsNeeded = contentArea[2];
            
            int contentHeight = rowsNeeded * UIConstants.INGREDIENT_SLOT_SIZE;
            int newTotalHeight = UIConstants.FOLDER_AREA_HEIGHT + 55 + contentHeight + 10 + contentArea[3];
            
            layoutManager.updateHeight(newTotalHeight);
            
            DebugLogger.debugValues(DebugLogger.Category.GUI_STATE, 
                "Resizing folder screen to fit {} rows (including 1 extra row), new height: {}, vertical offset: {}", 
                rowsNeeded, layoutManager.getHeight(), contentArea[3]);
            
            for (int i = startIndex; i < endIndex; i++) {
                int slotIndex = i - startIndex;
                int row = slotIndex / ingredientColumns;
                int col = slotIndex % ingredientColumns;
                
                int x = contentStartX + col * UIConstants.INGREDIENT_SLOT_SIZE;
                int y = contentStartY + row * UIConstants.INGREDIENT_SLOT_SIZE;
                
                ingredientSlots.add(new IngredientSlot(x, y, ingredients.get(i)));
            }
            
            updatePagination();
            return layoutManager.getHeight();
        }).orElse(UIConstants.FOLDER_AREA_HEIGHT + 10);
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
