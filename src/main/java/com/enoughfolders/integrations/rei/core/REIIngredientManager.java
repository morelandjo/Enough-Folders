package com.enoughfolders.integrations.rei.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.base.AbstractIngredientManager;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Manages ingredient conversion and rendering for REI integration.
 */
public class REIIngredientManager extends AbstractIngredientManager {
    
    /**
     * Creates a new REI ingredient manager.
     */
    public REIIngredientManager() {
        super("REI");
    }
    
    /**
     * Performs the actual conversion from stored ingredient to REI ingredient.
     *
     * @param storedIngredient The stored ingredient to convert
     * @return The REI ingredient, or null if conversion failed
     */
    @Override
    protected Object doGetIngredientFromStored(StoredIngredient storedIngredient) {
        try {
            // First try direct REI format lookup
            if (storedIngredient.getType().equals("net.minecraft.world.item.ItemStack")) {
                // Parse the item ID
                String itemId = storedIngredient.getValue();
                DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                    "Converting stored ingredient with ID: {}", itemId);
                
                // Extract registry name from the item ID
                if (itemId.contains("@")) {
                    itemId = itemId.substring(0, itemId.indexOf("@"));
                }
                
                // Get the item from the registry
                String namespace = itemId.substring(0, itemId.indexOf(":"));
                String path = itemId.substring(itemId.indexOf(":") + 1);
                ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(namespace, path);
                    
                Item item = BuiltInRegistries.ITEM.get(resourceLocation);
                
                if (item != null) {
                    ItemStack itemStack = new ItemStack(item);
                    
                    // Create an EntryStack from the ItemStack
                    Object entryStack = createREIEntryStack(itemStack);
                    if (entryStack != null) {
                        return entryStack;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error converting stored ingredient to REI ingredient", e);
            return null;
        }
    }
    
    /**
     * Creates an REI EntryStack from an ItemStack using reflection.
     * 
     * @param itemStack The ItemStack to convert
     * @return The REI EntryStack, or null if conversion failed
     */
    private Object createREIEntryStack(ItemStack itemStack) {
        try {
            Class<?> entryStackClass = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");
            Class<?> vanillaTypesClass = Class.forName("me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes");
            
            java.lang.reflect.Field itemField = vanillaTypesClass.getField("ITEM");
            Object itemType = itemField.get(null);
            
            java.lang.reflect.Method ofMethod = entryStackClass.getMethod("of", Object.class, Object.class);
            return ofMethod.invoke(null, itemType, itemStack);
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to create REI EntryStack", e);
            return null;
        }
    }
    
    /**
     * Performs the actual conversion from REI ingredient to StoredIngredient.
     *
     * @param ingredient The REI ingredient to store
     * @return The StoredIngredient, or null if conversion failed
     */
    @Override
    protected StoredIngredient doStoreIngredient(Object ingredient) {
        try {
            // Check if this is an REI EntryStack
            if (isREIEntryStack(ingredient)) {
                // Try to get the ItemStack from the EntryStack
                ItemStack itemStack = getItemStackFromREIEntryStack(ingredient);
                if (itemStack != null && !itemStack.isEmpty()) {
                    // Store the full item ID including metadata
                    String itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
                    
                    // Create a StoredIngredient from the ItemStack's ID
                    return new StoredIngredient(
                        "net.minecraft.world.item.ItemStack",
                        itemId
                    );
                }
            }
            
            return null;
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error storing REI ingredient", e);
            return null;
        }
    }
    
    /**
     * Checks if an object is an REI EntryStack.
     * 
     * @param obj The object to check
     * @return true if the object is an REI EntryStack, false otherwise
     */
    public static boolean isREIEntryStack(Object obj) {
        if (obj == null) {
            return false;
        }
        
        try {
            Class<?> entryStackClass = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");
            return entryStackClass.isInstance(obj);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Gets an ItemStack from an REI EntryStack.
     * 
     * @param entryStack The REI EntryStack
     * @return The ItemStack, or null if conversion failed
     */
    private ItemStack getItemStackFromREIEntryStack(Object entryStack) {
        try {
            Class<?> entryStackClass = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");
            
            // First check if it's an item stack
            java.lang.reflect.Method castMethod = entryStackClass.getMethod("cast");
            Object castEntryStack = castMethod.invoke(entryStack);
            
            java.lang.reflect.Method getValueMethod = entryStackClass.getMethod("getValue");
            Object value = getValueMethod.invoke(castEntryStack);
            
            if (value instanceof ItemStack) {
                return (ItemStack) value;
            }
            
            return null;
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to get ItemStack from REI EntryStack", e);
            return null;
        }
    }
    
    /**
     * Performs the actual conversion from ingredient to ItemStack for display.
     *
     * @param ingredient The ingredient to convert
     * @return The ItemStack representation, or empty stack if conversion failed
     */
    @Override
    protected ItemStack doGetItemStackForDisplay(Object ingredient) {
        try {
            if (isREIEntryStack(ingredient)) {
                return getItemStackFromREIEntryStack(ingredient);
            }
            
            return ItemStack.EMPTY;
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error getting ItemStack for REI ingredient", e);
            return ItemStack.EMPTY;
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
    public void renderIngredient(GuiGraphics graphics, StoredIngredient ingredient, int x, int y, int width, int height) {
        try {
            // Try to convert the stored ingredient back to an REI ingredient
            Optional<?> ingredientOpt = getIngredientFromStored(ingredient);
            if (ingredientOpt.isEmpty()) {
                return;
            }

            Object ingredientObj = ingredientOpt.get();
            
            // Try to use REI's rendering system first
            if (tryRenderWithREI(graphics, ingredientObj, x, y)) {
                return;
            }
            
            // Fallback to ItemStack rendering
            Optional<ItemStack> itemStackOpt = getItemStackForDisplay(ingredientObj);
            if (itemStackOpt.isPresent()) {
                ItemStack itemStack = itemStackOpt.get();
                
                // Draw the item using Minecraft's built-in renderer
                graphics.renderItem(itemStack, x, y);
                
                DebugLogger.debug(DebugLogger.Category.INTEGRATION, 
                    "Successfully rendered REI ingredient at " + x + "," + y + " using ItemStack fallback");
            } else {
                DebugLogger.debug(DebugLogger.Category.INTEGRATION, 
                    "Failed to get ItemStack for rendering REI ingredient");
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to render REI ingredient", e);
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Exception details: {}", e.getMessage());
        }
    }
    
    /**
     * Attempts to render the ingredient using REI's renderer system.
     * 
     * @param graphics The graphics context
     * @param ingredient The REI ingredient
     * @param x The x position
     * @param y The y position
     * @return true if rendering was successful, false if we should fall back
     */
    private boolean tryRenderWithREI(GuiGraphics graphics, Object ingredient, int x, int y) {
        try {
            // Check if this is an REI EntryStack
            if (!isREIEntryStack(ingredient)) {
                return false;
            }
            
            // Try to get the renderer from REI's API
            Class<?> entryStackClass = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");
            Class<?> entryRendererClass = Class.forName("me.shedaniel.rei.api.client.entry.renderer.EntryRenderer");
            
            // Get the renderer method
            java.lang.reflect.Method getRendererMethod = entryStackClass.getMethod("getRenderer");
            Object renderer = getRendererMethod.invoke(ingredient);
            
            if (renderer != null) {
                // Try to call the render method
                java.lang.reflect.Method renderMethod = entryRendererClass.getMethod("render", 
                    entryStackClass, GuiGraphics.class, 
                    net.minecraft.client.renderer.Rect2i.class, 
                    int.class, int.class, float.class);
                
                // Create a rectangle for the rendering bounds
                net.minecraft.client.renderer.Rect2i bounds = new net.minecraft.client.renderer.Rect2i(x, y, 16, 16);
                
                renderMethod.invoke(renderer, ingredient, graphics, bounds, x, y, 0.0f);
                
                DebugLogger.debug(DebugLogger.Category.INTEGRATION, 
                    "Successfully rendered REI ingredient using REI renderer");
                return true;
            }
        } catch (Exception e) {
            // Log debug info but don't throw - we'll fall back to ItemStack rendering
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, 
                "REI renderer not available, falling back to ItemStack: " + e.getMessage());
        }
        
        return false;
    }
}
