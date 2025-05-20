package com.enoughfolders.integrations.rei.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.ModIntegration;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Integration for Roughly Enough Items (REI) mod.
 * Provides functionality for ingredient drag and drop, recipe viewing, and more.
 */
public class REIIntegration implements ModIntegration {
    
    private boolean initialized = false;
    private boolean available = false;
    
    /**
     * Initialize the REI integration. This will check if REI is available
     * and set up the necessary hooks if it is.
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
            
            // Don't try to access REI runtime during initialization
            // Just mark as available since classes are present
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
                } else if (entryStack.getType() == me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes.FLUID) {
                    // Handle fluid entry - if your mod supports fluids
                    Object value = entryStack.getValue();
                    
                    DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                        "REI fluid entries not implemented yet: {}", 
                        value != null ? value.getClass().getName() : "null");
                    // TODO: Implement fluid handling if needed
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
                    } else {
                        EnoughFolders.LOGGER.error("Failed to get item from registry: {}", itemId);
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
     * This is used for drag and drop operations.
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
            
            // Log that we don't know how to convert this type
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
}
