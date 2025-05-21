package com.enoughfolders.client.gui;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.api.FolderTarget;
import com.enoughfolders.integrations.api.RecipeViewingIntegration;
import com.enoughfolders.integrations.jei.gui.targets.FolderButtonTarget;
import com.enoughfolders.integrations.rei.gui.targets.REIFolderTarget;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.gui.components.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages folder buttons in the FolderScreen.
 */
public class FolderButtonManager {
    private static final int FOLDER_WIDTH = 18;
    private static final int FOLDER_HEIGHT = 18;
    private static final int FOLDER_ROW_HEIGHT = 27;
    private static final int FOLDER_COLUMN_SPACING = 4;
    
    private final List<FolderButton> folderButtons = new ArrayList<>();
    private final FolderManager folderManager;
    private Button addFolderButton;
    private int folderRowsCount = 1;
    
    // Callback for folder clicked events
    private final Consumer<Folder> onFolderClickedCallback;
    
    // Position of the container
    private int leftPos;
    private int width;
    
    /**
     * Creates a new folder button manager.
     *
     * @param folderManager The folder manager
     * @param onFolderClickedCallback Callback for when a folder is clicked
     */
    public FolderButtonManager(FolderManager folderManager, Consumer<Folder> onFolderClickedCallback) {
        this.folderManager = folderManager;
        this.onFolderClickedCallback = onFolderClickedCallback;
    }
    
    /**
     * Sets the position and dimensions for layout.
     *
     * @param leftPos Left position
     * @param width Width of the container
     */
    public void setPositionAndDimensions(int leftPos, int width) {
        this.leftPos = leftPos;
        this.width = width;
    }
    
    /**
     * Creates the add folder button.
     *
     * @param x X position
     * @param y Y position
     * @param onAddFolderPressed Callback when the button is pressed
     * @return The add folder button
     */
    public Button createAddFolderButton(int x, int y, Button.OnPress onAddFolderPressed) {
        addFolderButton = new Button.Builder(
                net.minecraft.network.chat.Component.literal("+"), 
                onAddFolderPressed)
                .pos(x, y)
                .size(FOLDER_WIDTH, FOLDER_HEIGHT)
                .build();
        addFolderButton.active = true;
        
        return addFolderButton;
    }

    /**
     * Initializes the folder buttons in the UI.
     *
     * @param topPos Top position of the container
     * @param isAddingFolder Whether we're currently in add folder mode
     * @return The number of folder button rows
     */
    public int initFolderButtons(int topPos, boolean isAddingFolder) {
        folderButtons.clear();
        
        List<Folder> folders = folderManager.getFolders();
        
        int startX = leftPos + 5 + FOLDER_WIDTH + 5;
        int currentX = startX;
        int currentY = topPos + 5;
        int rowCount = 1;
        
        int availableWidth = width - 10;
        int singleFolderWidth = FOLDER_WIDTH + FOLDER_COLUMN_SPACING;
        
        int firstRowWidth = availableWidth - (FOLDER_WIDTH + 5);
        int foldersInFirstRow = Math.max(1, firstRowWidth / singleFolderWidth);
        
        int foldersPerRow = Math.max(1, availableWidth / singleFolderWidth);
        
        DebugLogger.debugValues(DebugLogger.Category.GUI_STATE, 
            "Dynamic layout: firstRow={} folders, subsequentRows={} folders, availWidth={}, folderWidth={}", 
            foldersInFirstRow, foldersPerRow, availableWidth, singleFolderWidth);
        
        if (isAddingFolder) {
            currentY += 20; // INPUT_FIELD_HEIGHT
        }
        
        for (int i = 0; i < folders.size(); i++) {
            Folder folder = folders.get(i);
            
            boolean isFirstRow = rowCount == 1;
            
            int positionInRow = isFirstRow ? i : (i - foldersInFirstRow) % foldersPerRow;
            
            if ((isFirstRow && i > 0 && i % foldersInFirstRow == 0) ||
                (!isFirstRow && positionInRow == 0)) {
                currentX = leftPos + 5;
                currentY += FOLDER_ROW_HEIGHT;
                rowCount++;
            }
            
            final Folder buttonFolder = folder;
            Button.OnPress onPressHandler = button -> onFolderClickedCallback.accept(buttonFolder);
            
            FolderButton button = new FolderButton(
                    currentX, 
                    currentY, 
                    FOLDER_WIDTH, 
                    FOLDER_HEIGHT, 
                    folder,
                    onPressHandler
            );
            folderButtons.add(button);
            
            currentX += FOLDER_WIDTH + FOLDER_COLUMN_SPACING;
        }
        
        this.folderRowsCount = rowCount;
        
        DebugLogger.debugValues(DebugLogger.Category.GUI_STATE,
            "Folder buttons initialized: {} folders in {} rows", folders.size(), folderRowsCount);
            
        return rowCount;
    }

    /**
     * Gets all folder buttons.
     *
     * @return List of folder buttons
     */
    public List<FolderButton> getFolderButtons() {
        return folderButtons;
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
     * Gets the number of folder button rows.
     *
     * @return The number of folder button rows
     */
    public int getFolderRowsCount() {
        return folderRowsCount;
    }
    
    /**
     * Gets folder targets for all folder buttons from a specific integration.
     *
     * @param <T> The type of folder targets to return
     * @param integrationClassName The fully qualified class name of the integration to use
     * @param targetClass The class of the target type to cast to
     * @return List of folder targets for drag-and-drop, or an empty list if the integration is not available
     */
    @SuppressWarnings("unchecked")
    private <T extends FolderTarget> List<T> getFolderTargets(String integrationClassName, Class<T> targetClass) {
        return IntegrationRegistry.getIntegrationByClassName(integrationClassName)
            .filter(integration -> integration instanceof RecipeViewingIntegration)
            .map(integration -> (RecipeViewingIntegration) integration)
            .filter(RecipeViewingIntegration::isAvailable)
            .map(integration -> {
                List<?> targets = integration.createFolderTargets(folderButtons);
                // Cast the targets to the requested type
                return (List<T>) targets;
            })
            .orElse(new ArrayList<>());
    }
    
    /**
     * Gets JEI-specific folder targets for all folder buttons.
     *
     * @return List of JEI folder button targets for drag-and-drop
     */
    public List<FolderButtonTarget> getJEIFolderTargets() {
        EnoughFolders.LOGGER.debug("Building JEI folder targets - Number of folder buttons available: {}", folderButtons.size());
        return getFolderTargets("com.enoughfolders.integrations.jei.core.JEIIntegrationCore", FolderButtonTarget.class);
    }
    
    /**
     * Gets REI-specific folder targets for all folder buttons.
     *
     * @return List of REI folder button targets for drag-and-drop
     */
    public List<REIFolderTarget> getREIFolderTargets() {
        EnoughFolders.LOGGER.debug("Building REI folder targets - Number of folder buttons available: {}", folderButtons.size());
        return getFolderTargets("com.enoughfolders.integrations.rei.core.REIIntegration", REIFolderTarget.class);
    }
    
    /**
     * Gets folder targets for all folder buttons
     *
     * @param integrationClassName The class name of the integration to use
     * @return List of folder targets for drag-and-drop
     */
    public List<? extends FolderTarget> getFolderTargetsForIntegration(String integrationClassName) {
        return IntegrationRegistry.getIntegrationByClassName(integrationClassName)
            .filter(integration -> integration instanceof RecipeViewingIntegration)
            .map(integration -> (RecipeViewingIntegration) integration)
            .filter(RecipeViewingIntegration::isAvailable)
            .map(integration -> integration.createFolderTargets(folderButtons))
            .orElse(new ArrayList<>());
    }
}
