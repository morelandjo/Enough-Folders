package com.enoughfolders.integrations.rei.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.ModIntegration;
import com.enoughfolders.integrations.api.IngredientDragProvider;
import com.enoughfolders.integrations.api.RecipeViewingIntegration;
import com.enoughfolders.integrations.rei.gui.handlers.REIFolderIngredientHandler;
import com.enoughfolders.integrations.rei.gui.targets.REIFolderTarget;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Integration with Roughly Enough Items (REI) mod.
 * <p>
 * This class provides functionality for integrating EnoughFolders with the REI recipe and item browser.
 * </p>
 */
public class REIIntegration implements ModIntegration, IngredientDragProvider, RecipeViewingIntegration {
    
    /**
     * Creates a new REI integration instance.
     */
    public REIIntegration() {
        // Default constructor
    }
    
    private boolean initialized = false;
    private boolean available = false;
    
    /**
     * Initialize the REI integration.
     */
    @Override
    public void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            // Check if REI API classes are available
            Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "REI classes found, enabling REI integration");
            
            available = true;
            
            // REI plugin system will handle registration automatically through annotations
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "REI integration initialized successfully");
            
        } catch (ClassNotFoundException e) {
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "REI classes not found, disabling REI integration");
            available = false;
        } catch (Exception e) {
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Error initializing REI integration: {}", e.getMessage());
            available = false;
        }
        
        initialized = true;
    }
    
    /**
     * Checks if the REI integration is available.
     * 
     * @return true if REI is installed and integration is available, false otherwise
     */
    @Override
    public boolean isAvailable() {
        return available;
    }
    
    /**
     * Gets the mod's display name.
     * 
     * @return The display name of the mod
     */
    @Override
    public String getModName() {
        return "Roughly Enough Items";
    }
    
    /**
     * Converts a StoredIngredient to a REI ingredient.
     * 
     * @param storedIngredient The stored ingredient to convert
     * @return An Optional containing the REI ingredient, or empty if conversion failed
     */
    @Override
    public Optional<?> getIngredientFromStored(StoredIngredient storedIngredient) {
        try {
            if (!isAvailable()) {
                DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                    "REI integration not available, can't convert stored ingredient");
                return Optional.empty();
            }
            
            // First try direct REI format lookup
            if (storedIngredient.getType().equals("net.minecraft.world.item.ItemStack")) {
                // Parse the item ID
                String itemId = storedIngredient.getValue();
                DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                    "Converting stored ingredient with ID: {}", itemId);
                
                // Extract registry name from the item ID
                if (itemId.contains("@")) {
                    itemId = itemId.substring(0, itemId.indexOf("@"));
                }
                
                // Get the item from the registry
                String namespace = itemId.substring(0, itemId.indexOf(":"));
                String path = itemId.substring(itemId.indexOf(":") + 1);
                net.minecraft.resources.ResourceLocation resourceLocation = 
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, path);
                    
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(resourceLocation);
                
                if (item != null) {
                    ItemStack itemStack = new ItemStack(item);
                    
                    // Create an EntryStack from the ItemStack
                    var entryStack = me.shedaniel.rei.api.common.entry.EntryStack.of(
                        me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes.ITEM, 
                        itemStack
                    );
                    
                    return Optional.of(entryStack);
                }
            }
            
            // If direct lookup failed, try cross-integration compatibility
            Optional<?> crossIntegrationResult = tryConvertFromOtherIntegration(storedIngredient);
            if (crossIntegrationResult.isPresent()) {
                return crossIntegrationResult;
            }
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error converting stored ingredient to REI ingredient: {}", e.getMessage());
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Exception details: {}", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Safely gets an entry stack from a list widget, with null checks.
     * 
     * @param listWidget The overlay list widget to get the focused stack from
     * @return The focused stack, or null if there is no focused stack or the widget is null
     */
    public me.shedaniel.rei.api.common.entry.EntryStack<?> getSafelyFocusedStack(me.shedaniel.rei.api.client.overlay.OverlayListWidget listWidget) {
        if (listWidget == null) {
            EnoughFolders.LOGGER.debug("List widget is null, cannot get focused stack");
            return null;
        }
        
        try {
            return listWidget.getFocusedStack();
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error getting focused stack: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Stores a REI ingredient as a StoredIngredient.
     * 
     * @param ingredient The REI ingredient to store
     * @return An Optional containing the StoredIngredient, or empty if conversion failed
     */
    @Override
    public Optional<StoredIngredient> storeIngredient(Object ingredient) {
        try {
            if (ingredient instanceof me.shedaniel.rei.api.common.entry.EntryStack<?> entryStack) {
                if (entryStack.getType() == me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes.ITEM) {
                    // Handle item entry
                    Object value = entryStack.getValue();
                    if (value instanceof net.minecraft.world.item.ItemStack itemStack) {
                        String typeClass = "net.minecraft.world.item.ItemStack";
                        String itemId = itemStack.getItem().toString();
                        
                        DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                            "Converting REI item entry to StoredIngredient with ID: {}", itemId);
                            
                        return Optional.of(new StoredIngredient(typeClass, itemId));
                    }
                }
                
                // Log unknown entry type
                String typeString = entryStack.getType() != null ? entryStack.getType().toString() : "null";
                DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                    "Unknown REI entry type: " + typeString);
            } else {
                String className = ingredient != null ? ingredient.getClass().getName() : "null";
                DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                    "Unknown ingredient type: " + className);
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error storing REI ingredient: {}", e.getMessage());
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Exception details: {}", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Gets an ItemStack for displaying a REI ingredient.
     * 
     * @param ingredient The REI ingredient to get an ItemStack for
     * @return An Optional containing the ItemStack, or empty if conversion failed
     */
    @Override
    public Optional<ItemStack> getItemStackForDisplay(Object ingredient) {
        try {
            if (!isAvailable()) {
                DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                    "REI integration not available, can't get item stack for display");
                return Optional.empty();
            }
            
            EnoughFolders.LOGGER.debug("Getting ItemStack for display from: {}", 
                ingredient != null ? ingredient.getClass().getName() : "null");
            
            if (ingredient instanceof me.shedaniel.rei.api.common.entry.EntryStack<?> entryStack) {
                if (entryStack.getType() == me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes.ITEM) {
                    Object value = entryStack.getValue();
                    if (value instanceof ItemStack itemStack) {
                        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                            "Successfully converted REI EntryStack to ItemStack for display");
                        return Optional.of(itemStack);
                    }
                }
            } else if (ingredient instanceof StoredIngredient storedIngredient) {
                // If it's a StoredIngredient, try to parse it directly
                if (storedIngredient.getType().equals("net.minecraft.world.item.ItemStack")) {
                    String itemId = storedIngredient.getValue();
                    
                    DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                        "Converting stored ingredient to ItemStack with ID: {}", itemId);
                    
                    // Extract registry name from the item ID
                    if (itemId.contains("@")) {
                        itemId = itemId.substring(0, itemId.indexOf("@"));
                    }
                    
                    // Get the item from the registry
                    String namespace = itemId.substring(0, itemId.indexOf(":"));
                    String path = itemId.substring(itemId.indexOf(":") + 1);
                    net.minecraft.resources.ResourceLocation resourceLocation = 
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, path);
                        
                    net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(resourceLocation);
                    
                    if (item != null) {
                        ItemStack itemStack = new ItemStack(item);
                        EnoughFolders.LOGGER.debug("Successfully created ItemStack for '{}': {}", 
                            itemId, itemStack.getItem().toString());
                        return Optional.of(itemStack);
                    } 
                } else {
                    EnoughFolders.LOGGER.debug("StoredIngredient has unsupported type: {}", 
                        storedIngredient.getType());
                }
            } else {
                EnoughFolders.LOGGER.debug("Unsupported ingredient type: {}", 
                    ingredient != null ? ingredient.getClass().getName() : "null");
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error getting ItemStack for display: {}", e.getMessage());
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Exception details: {}", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Gets the ingredient currently under the mouse in the REI UI.
     * 
     * @return Optional containing the ingredient, or empty if none is found
     */
    public Optional<Object> getDraggedIngredient() {
        try {
            if (!isAvailable()) {
                EnoughFolders.LOGGER.debug("REI integration not available, can't get dragged ingredient");
                return Optional.empty();
            }
            
            // Check if REI is fully initialized
            try {
                // Get REI runtime
                me.shedaniel.rei.api.client.REIRuntime runtime = me.shedaniel.rei.api.client.REIRuntime.getInstance();
                if (runtime == null) {
                    EnoughFolders.LOGGER.debug("REI runtime is null, can't get dragged ingredient");
                    return Optional.empty();
                }
                
                // Check if REI is visible
                if (!runtime.isOverlayVisible()) {
                    EnoughFolders.LOGGER.debug("REI overlay is not visible");
                    return Optional.empty();
                }
                
                // Get the REI overlay
                Optional<me.shedaniel.rei.api.client.overlay.ScreenOverlay> overlayOpt = runtime.getOverlay();
                if (overlayOpt.isEmpty()) {
                    EnoughFolders.LOGGER.debug("REI overlay is not available");
                    return Optional.empty();
                }
                
                me.shedaniel.rei.api.client.overlay.ScreenOverlay overlay = overlayOpt.get();
                
                // Try to get focused entry from the main entry list
                me.shedaniel.rei.api.client.overlay.OverlayListWidget entryList = overlay.getEntryList();
                if (entryList != null) {
                    me.shedaniel.rei.api.common.entry.EntryStack<?> focusedStack = getSafelyFocusedStack(entryList);
                    if (focusedStack != null && !focusedStack.isEmpty()) {
                        EnoughFolders.LOGGER.debug("Found focused entry in main entry list");
                        return Optional.of(focusedStack);
                    }
                }
                
                // If not found in main list, try favorites list
                Optional<me.shedaniel.rei.api.client.overlay.OverlayListWidget> favoritesListOpt = overlay.getFavoritesList();
                if (favoritesListOpt.isPresent()) {
                    me.shedaniel.rei.api.client.overlay.OverlayListWidget favoritesList = favoritesListOpt.get();
                    if (favoritesList != null) {
                        me.shedaniel.rei.api.common.entry.EntryStack<?> focusedStack = getSafelyFocusedStack(favoritesList);
                        if (focusedStack != null && !focusedStack.isEmpty()) {
                            EnoughFolders.LOGGER.debug("Found focused entry in favorites list");
                            return Optional.of(focusedStack);
                        }
                    }
                }
                
                EnoughFolders.LOGGER.debug("No focused entry found in any REI list");
            } catch (AssertionError e) {
                // This is expected if REI is not fully initialized
                EnoughFolders.LOGGER.debug("REI internals not initialized yet: {}", e.getMessage());
                return Optional.empty();
            }
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error getting dragged ingredient: {}", e.getMessage());
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Exception details: {}", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Shows recipes for the provided ingredient in the REI recipe GUI.
     *
     * @param ingredient The ingredient to show recipes for
     */
    public void showRecipes(Object ingredient) {
        try {
            if (!isAvailable()) {
                EnoughFolders.LOGGER.error("Cannot show recipes: REI integration not available");
                return;
            }
            
            // Save the current folder screen before showing recipes
            saveCurrentFolderScreen();
            
            // Check if REI is fully initialized
            try {
                // Get REI runtime
                me.shedaniel.rei.api.client.REIRuntime runtime = me.shedaniel.rei.api.client.REIRuntime.getInstance();
                if (runtime == null) {
                    EnoughFolders.LOGGER.error("Cannot show recipes: REI runtime is null");
                    return;
                }
                
                // Create a view screen for the ingredient
                if (ingredient instanceof me.shedaniel.rei.api.common.entry.EntryStack<?> entryStack) {
                    // Use entry stack directly
                    me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder()
                        .addRecipesFor(entryStack)
                        .open();
                    EnoughFolders.LOGGER.debug("Successfully showed recipes for EntryStack");
                } else {
                    // Try to convert the ingredient to an entry stack
                    Optional<me.shedaniel.rei.api.common.entry.EntryStack<?>> entryStackOpt = convertToEntryStack(ingredient);
                    if (entryStackOpt.isPresent()) {
                        me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder()
                            .addRecipesFor(entryStackOpt.get())
                            .open();
                        EnoughFolders.LOGGER.debug("Successfully showed recipes for converted ingredient");
                    } else {
                        EnoughFolders.LOGGER.error("Failed to convert ingredient for showing recipes");
                    }
                }
            } catch (AssertionError e) {
                // This is expected if REI is not fully initialized
                EnoughFolders.LOGGER.error("REI internals not initialized yet: {}", e.getMessage());
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error showing recipes for ingredient: {}", e.getMessage());
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Exception details: {}", e);
        }
    }
    
    /**
     * Shows usages for the provided ingredient in the REI recipe GUI.
     *
     * @param ingredient The ingredient to show usages for
     */
    public void showUses(Object ingredient) {
        try {
            if (!isAvailable()) {
                EnoughFolders.LOGGER.error("Cannot show uses: REI integration not available");
                return;
            }
            
            // Save the current folder screen before showing uses
            saveCurrentFolderScreen();
            
            // Check if REI is fully initialized
            try {
                // Get REI runtime
                me.shedaniel.rei.api.client.REIRuntime runtime = me.shedaniel.rei.api.client.REIRuntime.getInstance();
                if (runtime == null) {
                    EnoughFolders.LOGGER.error("Cannot show uses: REI runtime is null");
                    return;
                }
                
                // Create a view screen for the ingredient
                if (ingredient instanceof me.shedaniel.rei.api.common.entry.EntryStack<?> entryStack) {
                    // Use entry stack directly
                    me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder()
                        .addUsagesFor(entryStack)
                        .open();
                    EnoughFolders.LOGGER.debug("Successfully showed uses for EntryStack");
                } else {
                    // Try to convert the ingredient to an entry stack
                    Optional<me.shedaniel.rei.api.common.entry.EntryStack<?>> entryStackOpt = convertToEntryStack(ingredient);
                    if (entryStackOpt.isPresent()) {
                        me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder()
                            .addUsagesFor(entryStackOpt.get())
                            .open();
                        EnoughFolders.LOGGER.debug("Successfully showed uses for converted ingredient");
                    } else {
                        EnoughFolders.LOGGER.error("Failed to convert ingredient for showing uses");
                    }
                }
            } catch (AssertionError e) {
                // This is expected if REI is not fully initialized
                EnoughFolders.LOGGER.error("REI internals not initialized yet: {}", e.getMessage());
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error showing uses for ingredient: {}", e.getMessage());
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Exception details: {}", e);
        }
    }
    
    /**
     * Helper method to convert an object to a REI EntryStack.
     * 
     * @param ingredient The ingredient to convert
     * @return Optional containing the EntryStack, or empty if conversion failed
     */
    private Optional<me.shedaniel.rei.api.common.entry.EntryStack<?>> convertToEntryStack(Object ingredient) {
        try {
            if (ingredient instanceof me.shedaniel.rei.api.common.entry.EntryStack<?>) {
                EnoughFolders.LOGGER.debug("Ingredient is already an EntryStack, returning directly");
                return Optional.of((me.shedaniel.rei.api.common.entry.EntryStack<?>) ingredient);
            }
            
            // Convert ItemStack to EntryStack
            if (ingredient instanceof ItemStack itemStack) {
                EnoughFolders.LOGGER.debug("Converting ItemStack to EntryStack: {}", itemStack.getItem());
                return Optional.of(me.shedaniel.rei.api.common.entry.EntryStack.of(
                    me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes.ITEM, 
                    itemStack
                ));
            }
            
            // Try to convert StoredIngredient
            if (ingredient instanceof StoredIngredient storedIngredient) {
                EnoughFolders.LOGGER.debug("Converting StoredIngredient to EntryStack: {}", storedIngredient.getValue());
                Optional<?> reiIngredient = getIngredientFromStored(storedIngredient);
                if (reiIngredient.isPresent()) {
                    return convertToEntryStack(reiIngredient.get());
                }
            }
            
            EnoughFolders.LOGGER.error("Don't know how to convert ingredient of type: {}", 
                ingredient != null ? ingredient.getClass().getName() : "null");
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error converting ingredient to EntryStack: {}", e.getMessage());
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Exception details: {}", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Saves the current folder screen so it can be displayed on recipe screens.
     */
    private void saveCurrentFolderScreen() {
        net.minecraft.client.gui.screens.Screen currentScreen = net.minecraft.client.Minecraft.getInstance().screen;
        if (currentScreen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) {
            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen = 
                (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) currentScreen;
            
            com.enoughfolders.client.event.ClientEventHandler.getFolderScreen(containerScreen)
                .ifPresent(folderScreen -> {
                    // Initialize the folder screen with current dimensions
                    int screenWidth = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    int screenHeight = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    folderScreen.init(screenWidth, screenHeight);
                    
                    // Save the folder screen for later use
                    com.enoughfolders.integrations.rei.gui.handlers.REIRecipeGuiHandler.saveLastFolderScreen(folderScreen);
                    EnoughFolders.LOGGER.debug("Saved folder screen for REI recipe/usage view");
                });
        }
    }
    
    /**
     * Renders a stored ingredient in the GUI.
     *
     * @param graphics The graphics context to render with
     * @param ingredient The stored ingredient to render
     * @param x The x position to render at
     * @param y The y position to render at
     * @param width The width of the rendering area
     * @param height The height of the rendering area
     */
    @Override
    public void renderIngredient(net.minecraft.client.gui.GuiGraphics graphics, StoredIngredient ingredient, int x, int y, int width, int height) {
        if (!isAvailable()) {
            return;
        }

        try {
            // Try to convert the stored ingredient back to a REI ingredient
            Optional<?> ingredientOpt = getIngredientFromStored(ingredient);
            if (ingredientOpt.isEmpty()) {
                return;
            }

            // Get the item stack for display
            Optional<ItemStack> itemStackOpt = getItemStackForDisplay(ingredientOpt.get());
            if (itemStackOpt.isPresent()) {
                ItemStack itemStack = itemStackOpt.get();
                
                // Draw the item
                graphics.renderItem(itemStack, x, y);
                
                DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                    "Successfully rendered REI ingredient at " + x + "," + y);
            } else {
                DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                    "Failed to get ItemStack for rendering REI ingredient");
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to render REI ingredient", e);
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Exception details: {}", e);
        }
    }

    /**
     * Processes a drop of the currently dragged ingredient onto a folder.
     * Converts the REI ingredient to a StoredIngredient and adds it to the folder.
     * 
     * @param folder The folder to add the ingredient to
     * @return True if the drop was successful, false otherwise
     */
    @Override
    public boolean handleIngredientDrop(Folder folder) {
        Optional<Object> draggedIngredient = getDraggedIngredient();
        if (draggedIngredient.isEmpty()) {
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "No ingredient is being dragged");
            return false;
        }
        
        Object ingredient = draggedIngredient.get();
        DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
            "Processing REI dragged ingredient drop for folder: {}", folder.getName());
        
        // Convert ingredient to StoredIngredient
        Optional<StoredIngredient> storedIngredient = storeIngredient(ingredient);
        if (storedIngredient.isEmpty()) {
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Failed to convert ingredient to StoredIngredient");
            return false;
        }
        
        // Add ingredient to folder
        EnoughFolders.getInstance().getFolderManager().addIngredient(folder, storedIngredient.get());
        
        DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
            "Successfully added REI ingredient to folder: {}", folder.getName());
        return true;
    }
    
    /**
     * Gets the display name of the integration.
     * 
     * @return The display name
     */
    @Override
    public String getDisplayName() {
        return "REI";
    }
    
    /**
     * Connect a folder screen to REI for recipe viewing.
     * 
     * @param folderScreen The folder screen to connect
     * @param containerScreen The container screen
     */
    @Override
    public void connectToFolderScreen(FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen) {
        try {
            // Create handler if available
            if (isAvailable()) {
                REIFolderIngredientHandler handler = new REIFolderIngredientHandler(this);
                
                // Connect handler to folder screen
                handler.connectToFolderScreen(folderScreen, containerScreen);
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.debug("Could not connect folder to REI: {}", e.getMessage());
        }
    }
    
    /**
     * Save a folder screen to be used during recipe GUI navigation.
     * 
     * @param folderScreen The folder screen to save
     */
    @Override
    public void saveLastFolderScreen(FolderScreen folderScreen) {
        com.enoughfolders.integrations.rei.gui.handlers.REIRecipeGuiHandler.saveLastFolderScreen(folderScreen);
    }
    
    /**
     * Clear the saved folder screen when no longer needed.
     */
    @Override
    public void clearLastFolderScreen() {
        com.enoughfolders.integrations.rei.gui.handlers.REIRecipeGuiHandler.clearLastFolderScreen();
    }
    
    /**
     * Get the last folder screen saved for recipe GUI navigation.
     * 
     * @return Optional containing the folder screen if available
     */
    @Override
    public Optional<FolderScreen> getLastFolderScreen() {
        return com.enoughfolders.integrations.rei.gui.handlers.REIRecipeGuiHandler.getLastFolderScreen();
    }
    
    /**
     * Check if the given screen is a recipe screen for this integration.
     * 
     * @param screen The screen to check
     * @return True if it's a recipe screen for this integration, false otherwise
     */
    @Override
    public boolean isRecipeScreen(Screen screen) {
        if (screen == null || !isAvailable()) {
            return false;
        }
        
        // Check if the screen's class name contains REI recipe screen identifiers
        String className = screen.getClass().getName();
        boolean isREIScreen = className.contains("shedaniel.rei") && 
               (className.contains("RecipeScreen") || 
                className.contains("ViewSearchBuilder") || 
                className.contains("ViewsScreen") ||
                className.contains("DefaultDisplayViewingScreen"));
                
        if (isREIScreen) {
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Detected REI recipe screen: {}", className);
        }
        
        return isREIScreen;
    }
    
    /**
     * Check if the screen being closed is transitioning to a recipe screen for this integration.
     * 
     * @param screen The screen that's being closed
     * @return True if we're transitioning to a recipe screen, false otherwise
     */
    @Override
    public boolean isTransitioningToRecipeScreen(Screen screen) {
        if (!isAvailable()) {
            return false;
        }
        
        try {
            // Check the stack trace for REI-specific calls that indicate a recipe screen transition
            // Using centralized stack trace utility
            return com.enoughfolders.integrations.util.StackTraceUtils.isREIRecipeTransition();
        } catch (Exception e) {
            EnoughFolders.LOGGER.debug("Error checking for REI recipe transition: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Creates folder targets that can be used for ingredient drops from REI.
     * 
     * @param folderButtons The list of folder buttons to create targets for
     * @return A list of folder targets compatible with REI
     */
    @Override
    public List<REIFolderTarget> createFolderTargets(List<FolderButton> folderButtons) {
        EnoughFolders.LOGGER.debug("Creating REI folder targets - Number of folder buttons available: {}", 
            folderButtons.size());
        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Getting REI folder targets");
        
        List<REIFolderTarget> targets = com.enoughfolders.integrations.rei.gui.targets.REIFolderTargetFactory
            .getInstance()
            .createTargets(folderButtons);
        
        DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, "Created {} REI folder targets", targets.size());
        return targets;
    }
    
    /**
     * Attempts to convert ingredients stored by other integrations (EMI, JEI) to REI format.
     * This provides cross-integration compatibility when switching between integrations.
     *
     * @param storedIngredient The stored ingredient from another integration
     * @return Optional containing the converted ingredient, or empty if conversion failed
     */
    private Optional<?> tryConvertFromOtherIntegration(StoredIngredient storedIngredient) {
        try {
            String typeName = storedIngredient.getType();
            String value = storedIngredient.getValue();
            
            // Handle EMI format: type="minecraft:item", value="minecraft:stone"
            if ("minecraft:item".equals(typeName)) {
                return convertEMIItemToREI(value);
            }
            
            // Handle JEI format if needed (JEI uses full class names)
            if ("net.minecraft.world.item.ItemStack".equals(typeName)) {
                // This should be handled by the main method already, but included for completeness
                return convertJEIItemToREI(value);
            }
            
            // Add more integration formats as needed
            
            EnoughFolders.LOGGER.debug("No cross-integration conversion available for type: {}", typeName);
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to convert ingredient from other integration", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Converts an EMI-format item (ResourceLocation string) to a REI EntryStack.
     *
     * @param resourceLocationString The ResourceLocation string (e.g., "minecraft:stone")
     * @return Optional containing the converted EntryStack, or empty if conversion failed
     */
    private Optional<?> convertEMIItemToREI(String resourceLocationString) {
        try {
            EnoughFolders.LOGGER.debug("Converting EMI item to REI format: {}", resourceLocationString);
            
            // Parse the ResourceLocation
            net.minecraft.resources.ResourceLocation resourceLocation = 
                net.minecraft.resources.ResourceLocation.parse(resourceLocationString);
            
            // Get the item from the registry
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(resourceLocation);
            
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                // Create ItemStack and convert to REI EntryStack
                ItemStack itemStack = new ItemStack(item);
                var entryStack = me.shedaniel.rei.api.common.entry.EntryStack.of(
                    me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes.ITEM, 
                    itemStack
                );
                
                EnoughFolders.LOGGER.debug("Successfully converted EMI item to REI: {}", resourceLocationString);
                return Optional.of(entryStack);
            } else {
                EnoughFolders.LOGGER.warn("Failed to find item in registry: {}", resourceLocationString);
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to convert EMI item to REI format: {}", resourceLocationString, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Converts a JEI-format item UID to a REI EntryStack.
     * This handles the case where JEI items might be stored in a different format.
     *
     * @param itemUid The JEI item UID string
     * @return Optional containing the converted EntryStack, or empty if conversion failed
     */
    private Optional<?> convertJEIItemToREI(String itemUid) {
        try {
            // JEI UID format might be different, but for ItemStacks it's often the ResourceLocation
            // Extract registry name from the item ID (handle @ and other separators)
            String itemId = itemUid;
            if (itemId.contains("@")) {
                itemId = itemId.substring(0, itemId.indexOf("@"));
            }
            
            // Parse as ResourceLocation and convert similar to EMI
            return convertEMIItemToREI(itemId);
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to convert JEI item to REI format: {}", itemUid, e);
        }
        
        return Optional.empty();
    }
}
