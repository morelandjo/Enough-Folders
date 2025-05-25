package com.enoughfolders.client.gui;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.di.IntegrationProviderRegistry;
import com.enoughfolders.integrations.api.FolderTarget;
import com.enoughfolders.integrations.api.RecipeViewingIntegration;
import com.enoughfolders.integrations.jei.gui.targets.FolderButtonTarget;
import com.enoughfolders.integrations.rei.gui.targets.REIFolderTarget;
import com.enoughfolders.integrations.emi.gui.targets.EMIFolderTarget;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.gui.components.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages folder buttons in the FolderScreen.
 */
public class FolderButtonManager implements LayoutManager.LayoutChangeListener {
    
    private final List<FolderButton> folderButtons = new ArrayList<>();
    private final FolderManager folderManager;
    private Button addFolderButton;
    private int folderRowsCount = 1;
    
    // Callback for folder clicked events
    private final Consumer<Folder> onFolderClickedCallback;
    
    // Reference to the layout manager
    private final LayoutManager layoutManager;
    
    // State tracking
    private boolean isAddingFolder;
    
    /**
     * Creates a new folder button manager.
     *
     * @param folderManager The folder manager
     * @param onFolderClickedCallback Callback for when a folder is clicked
     * @param layoutManager The layout manager for position calculations
     */
    public FolderButtonManager(FolderManager folderManager, Consumer<Folder> onFolderClickedCallback, LayoutManager layoutManager) {
        this.folderManager = folderManager;
        this.onFolderClickedCallback = onFolderClickedCallback;
        this.layoutManager = layoutManager;
        this.layoutManager.addLayoutChangeListener(this);
    }
    
    /**
     * Creates the add folder button.
     *
     * @param onAddFolderPressed Callback when the button is pressed
     * @return The add folder button
     */
    public Button createAddFolderButton(Button.OnPress onAddFolderPressed) {
        int[] addButtonPos = layoutManager.getAddFolderButtonPosition();
        addFolderButton = new Button.Builder(
                net.minecraft.network.chat.Component.literal("+"), 
                onAddFolderPressed)
                .pos(addButtonPos[0], addButtonPos[1])
                .size(UIConstants.FOLDER_WIDTH, UIConstants.FOLDER_HEIGHT)
                .build();
        addFolderButton.active = true;
        
        return addFolderButton;
    }

    /**
     * Initializes the folder buttons in the UI.
     *
     * @param isAddingFolder Whether we're currently in add folder mode
     * @return The number of folder button rows
     */
    public int initFolderButtons(boolean isAddingFolder) {
        folderButtons.clear();
        this.isAddingFolder = isAddingFolder;
        
        List<Folder> folders = folderManager.getFolders();
        
        int[] buttonLayout = layoutManager.calculateFolderButtonLayout(isAddingFolder);
        int startX = buttonLayout[0];  // startX
        int currentY = buttonLayout[1]; // currentY
        int availableWidth = buttonLayout[2]; // availableWidth
        int firstRowWidth = buttonLayout[3]; // firstRowWidth
        
        int currentX = startX;
        int rowCount = 1;
        
        int singleFolderWidth = UIConstants.FOLDER_WIDTH + UIConstants.FOLDER_COLUMN_SPACING;
        
        int foldersInFirstRow = Math.max(1, firstRowWidth / singleFolderWidth);
        int foldersPerRow = Math.max(1, availableWidth / singleFolderWidth);
        
        DebugLogger.debugValues(DebugLogger.Category.GUI_STATE, 
            "Dynamic layout: firstRow={} folders, subsequentRows={} folders, availWidth={}, folderWidth={}", 
            foldersInFirstRow, foldersPerRow, availableWidth, singleFolderWidth);
        
        for (int i = 0; i < folders.size(); i++) {
            Folder folder = folders.get(i);
            
            boolean isFirstRow = rowCount == 1;
            
            int positionInRow = isFirstRow ? i : (i - foldersInFirstRow) % foldersPerRow;
            
            if ((isFirstRow && i > 0 && i % foldersInFirstRow == 0) ||
                (!isFirstRow && positionInRow == 0)) {
                currentX = layoutManager.getLeftPos() + 5;
                currentY += UIConstants.FOLDER_ROW_HEIGHT;
                rowCount++;
            }
            
            final Folder buttonFolder = folder;
            Button.OnPress onPressHandler = button -> onFolderClickedCallback.accept(buttonFolder);
            
            FolderButton button = new FolderButton(
                    currentX, 
                    currentY, 
                    UIConstants.FOLDER_WIDTH, 
                    UIConstants.FOLDER_HEIGHT, 
                    folder,
                    onPressHandler
            );
            folderButtons.add(button);
            
            currentX += UIConstants.FOLDER_WIDTH + UIConstants.FOLDER_COLUMN_SPACING;
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
     * Handles layout changes from the LayoutManager.
     */
    @Override
    public void onLayoutChanged() {
        // Re-initialize buttons when layout changes
        if (!folderButtons.isEmpty()) {
            initFolderButtons(isAddingFolder);
        }
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
        return IntegrationProviderRegistry.getIntegrationByClassName(integrationClassName)
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
        return getFolderTargets("com.enoughfolders.integrations.jei.core.JEIIntegration", FolderButtonTarget.class);
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
     * Gets EMI-specific folder targets for all folder buttons.
     *
     * @return List of EMI folder targets for drag-and-drop
     */
    public List<EMIFolderTarget> getEMIFolderTargets() {
        EnoughFolders.LOGGER.debug("Building EMI folder targets - Number of folder buttons available: {}", folderButtons.size());
        return getFolderTargets("com.enoughfolders.integrations.emi.core.EMIIntegration", EMIFolderTarget.class);
    }
    
    /**
     * Gets folder targets for all folder buttons
     *
     * @param integrationClassName The class name of the integration to use
     * @return List of folder targets for drag-and-drop
     */
    public List<? extends FolderTarget> getFolderTargetsForIntegration(String integrationClassName) {
        return IntegrationProviderRegistry.getIntegrationByClassName(integrationClassName)
            .filter(integration -> integration instanceof RecipeViewingIntegration)
            .map(integration -> (RecipeViewingIntegration) integration)
            .filter(RecipeViewingIntegration::isAvailable)
            .map(integration -> integration.createFolderTargets(folderButtons))
            .orElse(new ArrayList<>());
    }
}
