package com.enoughfolders.client.gui;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import com.enoughfolders.integrations.jei.gui.targets.FolderButtonTarget;
import com.enoughfolders.integrations.jei.gui.targets.FolderGhostIngredientTarget;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The main folder screen overlay that displays folders and their contents.
 * 
 */
public class FolderScreen implements FolderGhostIngredientTarget {
    private static final int INPUT_FIELD_HEIGHT = 20;
    private static final int JEI_WIDTH_REDUCTION = 20;
    private static final int FOLDER_AREA_HEIGHT = 22;
    
    /**
     * The container screen that this folder screen is overlaying
     */
    private final AbstractContainerScreen<?> parentScreen;
    
    /**
     * Reference to the folder manager for data access
     */
    private final FolderManager folderManager;
    
    /**
     * Component managers
     */
    private final FolderButtonManager buttonManager;
    private final IngredientGridManager gridManager;
    private final FolderScreenRenderer renderer;
    private final FolderInputHandler inputHandler;
    
    /**
     * UI controls for folder management
     */
    private Button deleteButton;
    private EditBox newFolderNameInput;
    
    /**
     * State variables for the folder UI
     */
    private boolean isAddingFolder = false;
    
    /**
     * Position and size of the folder screen
     */
    private int leftPos;
    private int topPos;
    private int width;
    private int height;
    
    /**
     * Creates a new folder screen overlay for the given parent screen.
     *
     * @param parentScreen The container screen to overlay
     */
    public FolderScreen(AbstractContainerScreen<?> parentScreen) {

        DebugLogger.debug(DebugLogger.Category.INITIALIZATION, "FolderScreen constructor called with: " + parentScreen.getClass().getName());
        
        this.parentScreen = parentScreen;
        this.folderManager = EnoughFolders.getInstance().getFolderManager();
        
        // Initialize component managers
        this.buttonManager = new FolderButtonManager(folderManager, this::onFolderClicked);
        this.gridManager = new IngredientGridManager(() -> folderManager.getActiveFolder());
        this.renderer = new FolderScreenRenderer(parentScreen, () -> folderManager.getActiveFolder());
        this.inputHandler = new FolderInputHandler(this::createNewFolder, this::toggleAddFolderMode);
    }
    
    /**
     * Initializes or reinitializes the folder screen with the given dimensions.
     *
     * @param parentWidth The width of the parent screen
     * @param parentHeight The height of the parent screen
     */
    public void init(int parentWidth, int parentHeight) {

        DebugLogger.debugValues(DebugLogger.Category.INITIALIZATION, "FolderScreen.init called with width: {}, height: {}", 
            parentWidth, parentHeight);
        
        boolean wasAddingFolder = isAddingFolder;
        String currentInputText = newFolderNameInput != null ? newFolderNameInput.getValue() : "";
        boolean inputHadFocus = newFolderNameInput != null && newFolderNameInput.isFocused();
        
        int parentLeftPos = 0;
        if (parentScreen != null) {
            int standardContainerWidth = 176;
            parentLeftPos = (parentWidth - standardContainerWidth) / 2;
            DebugLogger.debugValue(DebugLogger.Category.GUI_STATE, "Estimated parent screen left position: {}", parentLeftPos);
        }
        
        boolean isJeiRecipeGuiOpen = false;
        Optional<JEIIntegration> jeiIntegration = IntegrationRegistry.getIntegration(JEIIntegration.class);
        if (jeiIntegration.isPresent()) {
            isJeiRecipeGuiOpen = jeiIntegration.get().isRecipeGuiOpen();
            if (isJeiRecipeGuiOpen) {
                DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEI recipe GUI is open, reducing folder UI width");
            }
        }
        
        int maxWidth = Math.min(parentWidth - 40, 387);
        
        if (parentLeftPos > 0) {
            int originalWidth = maxWidth;
            maxWidth = Math.min(maxWidth, parentLeftPos - 20);
            if (originalWidth != maxWidth) {
                DebugLogger.debugValues(DebugLogger.Category.GUI_STATE,
                    "Width limited to avoid parent screen overlap, from {} to {}", originalWidth, maxWidth);
            }
        }
        
        if (isJeiRecipeGuiOpen) {
            int originalWidth = maxWidth;
            maxWidth = Math.max(70, maxWidth - JEI_WIDTH_REDUCTION);
            DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION, 
                "Reduced width for JEI recipe GUI by {}, original: {}, new width: {}", 
                JEI_WIDTH_REDUCTION, originalWidth, maxWidth);
        }
        
        width = maxWidth;
        leftPos = 5;
        topPos = 5;
        
        // Check for FTB sidebar overlap and adjust position if necessary
        adjustPositionForFTBSidebar();
        
        // Set position and dimensions for all component managers
        buttonManager.setPositionAndDimensions(leftPos, width);
        gridManager.setPositionAndDimensions(leftPos, topPos, width);
        renderer.setPositionAndDimensions(leftPos, topPos, width, 0); // Height will be updated later
        inputHandler.setPositionAndDimensions(leftPos, topPos, width, 0); // Height will be updated later
        
        // Create the add folder button
        buttonManager.createAddFolderButton(
                leftPos + 5, 
                topPos + 5,
                button -> toggleAddFolderMode());
                
        // Initialize folder buttons
        int folderRowsCount = buttonManager.initFolderButtons(topPos, isAddingFolder);
        
        // Calculate base dimensions
        boolean hasActiveFolder = folderManager.getActiveFolder().isPresent();
        int folderRowsHeight = FOLDER_AREA_HEIGHT;
        if (folderRowsCount > 1) {
            folderRowsHeight += (folderRowsCount - 1) * 27; // FOLDER_ROW_HEIGHT
        }
        
        // Calculate initial height
        if (hasActiveFolder) {
            height = folderRowsHeight + 20 + 72 + 5; // 72 is CONTENT_AREA_HEIGHT
        } else {
            height = folderRowsHeight + 10;
        }

        if (wasAddingFolder) {
            height += INPUT_FIELD_HEIGHT;
        }
        
        // Create the delete button
        deleteButton = new Button.Builder(Component.literal("X"), button -> deleteCurrentFolder())
                .pos(leftPos + width - 25, topPos + FOLDER_AREA_HEIGHT + 5)
                .size(20, 20)
                .build();
        
        // Create pagination buttons for ingredient grid
        gridManager.createPaginationButtons(
                button -> {
                    gridManager.previousPage();
                    refreshIngredientSlots();
                },
                button -> {
                    gridManager.nextPage();
                    refreshIngredientSlots();
                });
        
        // Create the folder name input field
        newFolderNameInput = new EditBox(
                Minecraft.getInstance().font, 
                leftPos + 30, 
                topPos + 7, 
                width - 35, 
                16, 
                Component.literal("Folder Name")
        );
        newFolderNameInput.setMaxLength(20);
        
        isAddingFolder = wasAddingFolder;
        newFolderNameInput.setValue(currentInputText);
        newFolderNameInput.setVisible(isAddingFolder);
        if (inputHadFocus) {
            newFolderNameInput.setFocused(true);
        }
        
        DebugLogger.debugValues(DebugLogger.Category.GUI_STATE, 
            "Input box state restored: visible={}, text={}", isAddingFolder, currentInputText);
        
        // Initialize ingredient slots and update final dimensions
        refreshIngredientSlots();
    }
    
    /**
     * Refreshes the ingredient slots for the active folder.
     */
    private void refreshIngredientSlots() {
        int newHeight = gridManager.refreshIngredientSlots(isAddingFolder, buttonManager.getFolderRowsCount());
        
        // Update height for all components
        height = newHeight;
        renderer.updateHeight(height);
        inputHandler.updateHeight(height);
    }
    
    /**
     * Toggles the folder creation mode on or off.
     */
    private void toggleAddFolderMode() {
        isAddingFolder = !isAddingFolder;
        newFolderNameInput.setVisible(isAddingFolder);
        
        if (isAddingFolder) {
            newFolderNameInput.setFocused(true);
        }
        
        DebugLogger.debugValues(DebugLogger.Category.GUI_STATE, 
            "Add folder mode toggled to: {}, input box visibility: {}", 
            isAddingFolder, newFolderNameInput.isVisible());
        
        if (!isAddingFolder && !newFolderNameInput.getValue().isEmpty()) {
            createNewFolder(newFolderNameInput.getValue());
            newFolderNameInput.setValue("");
        }
        
        // Re-initialize buttons with new layout
        buttonManager.initFolderButtons(topPos, isAddingFolder);
        refreshIngredientSlots();
    }
    
    /**
     * Creates a new folder with the given name.
     *
     * @param name The name for the new folder
     */
    private void createNewFolder(String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        
        folderManager.createFolder(name.trim());
        buttonManager.initFolderButtons(topPos, isAddingFolder);
    }
    
    /**
     * Handles clicks on folder buttons.
     *
     * @param folder The folder that was clicked
     */
    private void onFolderClicked(Folder folder) {
        if (folder.isActive()) {
            folderManager.clearActiveFolder();
            DebugLogger.debug(DebugLogger.Category.INPUT, "Active folder deactivated: " + folder.getName());
        } else {
            folderManager.setActiveFolder(folder);
            DebugLogger.debug(DebugLogger.Category.INPUT, "Folder activated: " + folder.getName());
        }
        refreshIngredientSlots();
    }
    
    /**
     * Deletes the currently active folder.
     */
    private void deleteCurrentFolder() {
        folderManager.getActiveFolder().ifPresent(folder -> {
            folderManager.deleteFolder(folder);
            folderManager.clearActiveFolder();
            buttonManager.initFolderButtons(topPos, isAddingFolder);
            refreshIngredientSlots();
        });
    }
    
    /**
     * Renders the folder screen.
     *
     * @param graphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param partialTick The partial tick time
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        
        renderer.render(
            graphics,
            mouseX,
            mouseY,
            partialTick,
            buttonManager.getFolderButtons(),
            gridManager.getIngredientSlots(),
            buttonManager.getAddFolderButton(),
            deleteButton,
            gridManager.getPrevPageButton(),
            gridManager.getNextPageButton(),
            newFolderNameInput,
            isAddingFolder,
            gridManager.getCurrentPage(),
            gridManager.getTotalPages(),
            buttonManager.getFolderRowsCount()
        );
    }
    
    /**
     * Handles mouse click events on the folder screen.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param button The mouse button that was clicked
     * @return true if the click was handled, false otherwise
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Process clicks on ingredient slots with custom handlers
        List<IngredientSlot> slots = gridManager.getIngredientSlots();
        for (IngredientSlot slot : slots) {
            if (slot.isHovered((int)mouseX, (int)mouseY)) {
                if (inputHandler.processIngredientClick(this, slot, mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        
        // Fall back to standard input handling
        return inputHandler.mouseClicked(
            mouseX,
            mouseY,
            button,
            buttonManager.getFolderButtons(),
            gridManager.getIngredientSlots(),
            buttonManager.getAddFolderButton(),
            deleteButton,
            gridManager.getPrevPageButton(),
            gridManager.getNextPageButton(),
            newFolderNameInput,
            isAddingFolder,
            folderManager.getActiveFolder().isPresent()
        );
    }
    
    /**
     * Handles mouse release events on the folder screen.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param button The mouse button that was released
     * @return true if the release was handled, false otherwise
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return inputHandler.mouseReleased(
            mouseX,
            mouseY,
            button,
            buttonManager.getFolderButtons(),
            this::onIngredientAdded
        );
    }
    
    /**
     * Handles keyboard key press events.
     *
     * @param keyCode The key code
     * @param scanCode The scan code
     * @param modifiers The modifier keys
     * @return true if the key press was handled, false otherwise
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return inputHandler.keyPressed(
            keyCode,
            scanCode,
            modifiers,
            isAddingFolder,
            newFolderNameInput
        );
    }
    
    /**
     * Handles character input events.
     *
     * @param codePoint The character code point
     * @param modifiers The modifier keys
     * @return true if the character input was handled, false otherwise
     */
    public boolean charTyped(char codePoint, int modifiers) {
        return inputHandler.charTyped(
            codePoint,
            modifiers,
            isAddingFolder,
            newFolderNameInput
        );
    }
    
    /**
     * Checks if the folder creation mode is active.
     *
     * @return true if the user is currently adding a new folder, false otherwise
     */
    public boolean isAddingFolder() {
        return isAddingFolder;
    }
    
    /**
     * Checks if the folder name input field has focus.
     *
     * @return true if the input field is focused, false otherwise
     */
    public boolean isInputFocused() {
        return newFolderNameInput != null && newFolderNameInput.isFocused();
    }
    
    /**
     * Checks if a point is within the folder screen's bounds.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @return true if the point is within the folder screen, false otherwise
     */
    public boolean isVisible(double mouseX, double mouseY) {
        return inputHandler.isVisible(mouseX, mouseY);
    }
    
    /**
     * Gets the area where ingredients can be dropped in the active folder.
     *
     * @return A rectangle representing the drop area
     */
    @Override
    public Rect2i getContentDropArea() {
        return gridManager.getContentDropArea(isAddingFolder, buttonManager.getFolderRowsCount());
    }
    
    /**
     * Called when an ingredient is added to a folder via drag-and-drop.
     */
    @Override
    public void onIngredientAdded() {
        refreshIngredientSlots();
    }

    /**
     * Gets the total area occupied by the folder screen.
     *
     * @return A rectangle representing the screen's bounds
     */
    public Rect2i getScreenArea() {
        return new Rect2i(leftPos, topPos, width, height);
    }
    
    /**
     * Gets all folder buttons for drop handling.
     *
     * @return List of all folder buttons
     */
    public List<FolderButton> getFolderButtons() {
        return buttonManager.getFolderButtons();
    }
    
    /**
     * Gets drop targets for all folder buttons.
     *
     * @return List of folder button targets for drag-and-drop
     */
    @Override
    public List<FolderButtonTarget> getFolderButtonTargets() {
        return buttonManager.getFolderButtonTargets();
    }

    /**
     * Interface for ingredient click handlers.
     */
    @FunctionalInterface
    public interface IngredientClickHandler {
        /**
         * Handle a click on an ingredient slot.
         * 
         * @param slot The slot that was clicked
         * @param button The mouse button used (0 = left, 1 = right)
         * @param shift Whether shift was held
         * @param ctrl Whether ctrl was held
         * @return true if the click was handled, false otherwise
         */
        boolean handle(IngredientSlot slot, int button, boolean shift, boolean ctrl);
    }
    
    private List<IngredientClickHandler> ingredientClickHandlers = new ArrayList<>();
    
    /**
     * Register a handler for ingredient clicks.
     * 
     * @param handler The handler to register
     */
    public void registerIngredientClickHandler(IngredientClickHandler handler) {
        ingredientClickHandlers.add(handler);
    }
    
    /**
     * Notify all registered ingredient click handlers.
     * 
     * @param slot The slot that was clicked
     * @param button The mouse button used
     * @param shift Whether shift was held
     * @param ctrl Whether ctrl was held
     * @return true if any handler processed the click, false otherwise
     */
    public boolean notifyIngredientClickHandlers(IngredientSlot slot, int button, boolean shift, boolean ctrl) {
        for (IngredientClickHandler handler : ingredientClickHandlers) {
            if (handler.handle(slot, button, shift, ctrl)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if FTB Library is loaded and if the sidebar would overlap with our folder GUI.
     * Adjusts the position if necessary.
     */
    private void adjustPositionForFTBSidebar() {
        // Check for FTB Library integration
        if (com.enoughfolders.integrations.ftb.FTBIntegration.isFTBLibraryLoaded()) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "Checking for FTB sidebar overlap");
            
            // Create a rectangle representing our current folder GUI position
            Rect2i folderRect = new Rect2i(leftPos, topPos, width, 100); // Use approximate height
            
            // Ask FTB integration to adjust the position if needed
            Rect2i adjustedRect = com.enoughfolders.integrations.ftb.FTBIntegration.avoidExclusionAreas(folderRect);
            
            // If position was adjusted, update our position
            if (adjustedRect.getY() != topPos) {
                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION,
                    "FTB sidebar detected, adjusted Y position from {} to {}", 
                    topPos, adjustedRect.getY());
                
                topPos = adjustedRect.getY();
            }
        }
    }
    
    /**
     * Gets all ingredient slots in the folder grid.
     *
     * @return List of all ingredient slots
     */
    public List<IngredientSlot> getIngredientSlots() {
        return gridManager.getIngredientSlots();
    }
    
    /**
     * Gets the entire folder UI area which can be used as a drop target.
     * This includes the entire colored rectangle where folders and ingredients are displayed.
     *
     * @return A rectangle representing the entire folder UI area
     */
    public Rect2i getEntireFolderArea() {
        return new Rect2i(leftPos, topPos, width, height);
    }
}
