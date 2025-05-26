package com.enoughfolders.client.gui;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.data.FolderContentState;
import com.enoughfolders.client.data.NavigationControls;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.client.integration.IntegrationHandler;
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

/**
 * The main folder screen overlay that displays folders and their contents.
 * 
 */
public class FolderScreen {
    
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
    private final LayoutManager layoutManager;
    
    /**
     * Integration handler for recipe viewing mods
     */
    private final IntegrationHandler integrationHandler;
    
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
        
        // Initialize the layout manager first
        this.layoutManager = new LayoutManager(() -> folderManager.getActiveFolder());
        
        // Initialize component managers
        this.buttonManager = new FolderButtonManager(folderManager, this::onFolderClicked, layoutManager);
        this.gridManager = new IngredientGridManager(() -> folderManager.getActiveFolder(), layoutManager);
        
        // Create render context with all necessary components
        com.enoughfolders.client.data.RenderContext renderContext = 
            new com.enoughfolders.client.data.RenderContext(
                parentScreen,
                folderManager,
                buttonManager,
                gridManager
            );
        
        this.renderer = new FolderScreenRenderer(renderContext);
        this.inputHandler = new FolderInputHandler(this::createNewFolder, this::toggleAddFolderMode);
        
        // Initialize the integration handler
        this.integrationHandler = new IntegrationHandler(this);
        
        // Add layout change listeners
        this.layoutManager.addLayoutChangeListener(() -> {
            // Update local position and size variables for backward compatibility
            leftPos = layoutManager.getLeftPos();
            topPos = layoutManager.getTopPos();
            width = layoutManager.getWidth();
            height = layoutManager.getHeight();
        });
        
        // Initialize integrations for this folder screen
        this.integrationHandler.initIntegrations(parentScreen);
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
        
        // Calculate initial dimensions using the layout manager
        layoutManager.calculateInitialDimensions(parentWidth, parentHeight);
        
        // Update local position and size variables
        leftPos = layoutManager.getLeftPos();
        topPos = layoutManager.getTopPos();
        width = layoutManager.getWidth();
        height = layoutManager.getHeight();
        
        // Set position and dimensions for component managers that don't use LayoutManager yet
        renderer.setPositionAndDimensions(leftPos, topPos, width, 0);
        inputHandler.setPositionAndDimensions(leftPos, topPos, width, 0);
        
        // Create the add folder button using LayoutManager
        buttonManager.createAddFolderButton(button -> toggleAddFolderMode());
                
        // Initialize folder buttons
        layoutManager.setIsAddingFolder(wasAddingFolder);
        int folderRowsCount = buttonManager.initFolderButtons(wasAddingFolder);
        layoutManager.setFolderRowsCount(folderRowsCount);
        
        // Create the delete button
        int[] deleteButtonPos = layoutManager.getDeleteButtonPosition();
        deleteButton = new Button.Builder(Component.literal("X"), button -> deleteCurrentFolder())
                .pos(deleteButtonPos[0], deleteButtonPos[1])
                .size(20, 20)
                .build();
        
        // Create pagination buttons for ingredient grid
        layoutManager.getPaginationButtonPositions(isAddingFolder);
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
        int[] inputFieldPos = layoutManager.getFolderNameInputPosition();
        newFolderNameInput = new EditBox(
                Minecraft.getInstance().font, 
                inputFieldPos[0],
                inputFieldPos[1],
                inputFieldPos[2], 
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
        
        // Register an ingredient click handler for showing recipes/uses
        registerIngredientClickHandler((slot, mouseButton, shift, ctrl) -> {
            if (slot.hasIngredient()) {
                // Left click for recipes, right click for uses
                if (mouseButton == 0) {
                    return integrationHandler.showRecipes(slot.getIngredient());
                } else if (mouseButton == 1) {
                    return integrationHandler.showUses(slot.getIngredient());
                }
            }
            return false;
        });
    }
    
    /**
     * Refreshes the ingredient slots for the active folder.
     */
    private void refreshIngredientSlots() {
        int newHeight = gridManager.refreshIngredientSlots(isAddingFolder, buttonManager.getFolderRowsCount());
        
        // Update height for all components
        layoutManager.updateHeight(newHeight);
        height = layoutManager.getHeight();
        renderer.updateHeight(height);
        inputHandler.updateHeight(height);
    }
    
    /**
     * Toggles the folder creation mode on or off.
     */
    private void toggleAddFolderMode() {
        isAddingFolder = !isAddingFolder;
        layoutManager.setIsAddingFolder(isAddingFolder);
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
        buttonManager.initFolderButtons(isAddingFolder);
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
        buttonManager.initFolderButtons(isAddingFolder);
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
            buttonManager.initFolderButtons(isAddingFolder);
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
        // Create navigation controls with all UI control elements
        NavigationControls controls = 
            new NavigationControls(
                buttonManager.getAddFolderButton(),
                deleteButton,
                gridManager.getPrevPageButton(),
                gridManager.getNextPageButton(),
                newFolderNameInput
            );
        
        // Render using the RenderContext
        renderer.renderWithContext(graphics, mouseX, mouseY, partialTick, isAddingFolder, controls);
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
        
        // Create content state with folder buttons and ingredient slots
        FolderContentState contentState = 
            new FolderContentState(
                isAddingFolder,
                gridManager.getCurrentPage(),
                gridManager.getTotalPages(),
                buttonManager.getFolderRowsCount(),
                buttonManager.getFolderButtons(),
                gridManager.getIngredientSlots()
            );
        
        // Create navigation controls with all UI control elements
        NavigationControls controls = 
            new NavigationControls(
                buttonManager.getAddFolderButton(),
                deleteButton,
                gridManager.getPrevPageButton(),
                gridManager.getNextPageButton(),
                newFolderNameInput
            );
        
        // Fall back to standard input handling using data objects
        return inputHandler.mouseClicked(
            mouseX,
            mouseY,
            button,
            contentState,
            controls,
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
        // Create content state with folder buttons and ingredient slots
        FolderContentState contentState = 
            new FolderContentState(
                isAddingFolder,
                gridManager.getCurrentPage(),
                gridManager.getTotalPages(),
                buttonManager.getFolderRowsCount(),
                buttonManager.getFolderButtons(),
                gridManager.getIngredientSlots()
            );
            
        return inputHandler.mouseReleased(
            mouseX,
            mouseY,
            button,
            contentState,
            this::onIngredientAdded,
            this
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
        // Create content state
        FolderContentState contentState = 
            new FolderContentState(
                isAddingFolder,
                gridManager.getCurrentPage(),
                gridManager.getTotalPages(),
                buttonManager.getFolderRowsCount(),
                buttonManager.getFolderButtons(),
                gridManager.getIngredientSlots()
            );
        
        // Create navigation controls
        NavigationControls controls = 
            new NavigationControls(
                buttonManager.getAddFolderButton(),
                deleteButton,
                gridManager.getPrevPageButton(),
                gridManager.getNextPageButton(),
                newFolderNameInput
            );
            
        return inputHandler.keyPressed(
            keyCode,
            scanCode,
            modifiers,
            contentState,
            controls
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
        // Create content state
        FolderContentState contentState = 
            new FolderContentState(
                isAddingFolder,
                gridManager.getCurrentPage(),
                gridManager.getTotalPages(),
                buttonManager.getFolderRowsCount(),
                buttonManager.getFolderButtons(),
                gridManager.getIngredientSlots()
            );
        
        // Create navigation controls
        NavigationControls controls = 
            new NavigationControls(
                buttonManager.getAddFolderButton(),
                deleteButton,
                gridManager.getPrevPageButton(),
                gridManager.getNextPageButton(),
                newFolderNameInput
            );
            
        return inputHandler.charTyped(
            codePoint,
            modifiers,
            contentState,
            controls
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
        return layoutManager.isPointInside(mouseX, mouseY);
    }
    
    /**
     * Refreshes the ingredient grid after ingredients have been added or removed.
     */
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
     * Gets all ingredient slots in the folder grid.
     *
     * @return List of all ingredient slots
     */
    public List<IngredientSlot> getIngredientSlots() {
        return gridManager.getIngredientSlots();
    }
    
    /**
     * Gets the entire folder UI area which can be used as a drop target.
     *
     * @return A rectangle representing the entire folder UI area
     */
    public Rect2i getEntireFolderArea() {
        return new Rect2i(leftPos, topPos, width, height);
    }
    
    /**
     * Gets the integration handler for this folder screen.
     *
     * @return The integration handler
     */
    public IntegrationHandler getIntegrationHandler() {
        return integrationHandler;
    }
    
    /**
     * Gets the layout manager for this folder screen.
     * 
     * @return The layout manager
     */
    public LayoutManager getLayoutManager() {
        return layoutManager;
    }
}
