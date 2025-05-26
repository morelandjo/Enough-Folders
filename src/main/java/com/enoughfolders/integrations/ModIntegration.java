package com.enoughfolders.integrations;

import com.enoughfolders.data.StoredIngredient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Base interface for mod integrations.
 */
public interface ModIntegration {
    /**
     * Gets the name of the integration mod.
     * 
     * @return The display name of the mod integration
     */
    String getModName();
    
    /**
     * Checks if the integration is available/loaded.
     * 
     * @return True if the mod is loaded and integration is available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Converts a StoredIngredient to an actual ingredient object.
     * 
     * @param storedIngredient The stored ingredient to convert
     * @return Optional containing the mod-specific ingredient object, or empty if conversion failed
     */
    Optional<?> getIngredientFromStored(StoredIngredient storedIngredient);
    
    /**
     * Converts an ingredient object to a StoredIngredient.
     * 
     * @param ingredient The mod-specific ingredient object to convert
     * @return Optional containing the StoredIngredient, or empty if conversion failed
     */
    Optional<StoredIngredient> storeIngredient(Object ingredient);
    
    /**
     * Gets an ItemStack representation of an ingredient for rendering.
     * 
     * @param ingredient The ingredient to convert to an ItemStack
     * @return Optional containing the ItemStack representation, or empty if conversion failed
     */
    Optional<ItemStack> getItemStackForDisplay(Object ingredient);
    
    /**
     * Register listeners and hooks with the integrated mod.
     */
    void initialize();
    
    /**
     * Register drag and drop support with the integrated mod.
     * NOTE: Drag and drop functionality has been removed - this method is kept for compatibility.
     */
    default void registerDragAndDrop() {
        // Drag and drop functionality has been removed - method kept for compatibility
    }
    
    /**
     * Check if this integration can handle the given ingredient.
     * 
     * @param ingredient The stored ingredient to check
     * @return True if this integration can handle the ingredient, false otherwise
     */
    default boolean canHandleIngredient(StoredIngredient ingredient) {
        return getIngredientFromStored(ingredient).isPresent();
    }
    
    /**
     * Render an ingredient using the integration.
     * 
     * @param graphics The GUI graphics context to render with
     * @param ingredient The ingredient to render
     * @param x The x position to render at
     * @param y The y position to render at
     * @param width The width of the rendering area
     * @param height The height of the rendering area
     */
    default void renderIngredient(GuiGraphics graphics, StoredIngredient ingredient, int x, int y, int width, int height) {
        // Default implementation does nothing
    }
    
    /**
     * Convert an object to a StoredIngredient using this integration.
     * 
     * @param ingredientObj The object to convert to a stored ingredient
     * @return The created StoredIngredient, or null if conversion failed
     */
    default StoredIngredient createStoredIngredient(Object ingredientObj) {
        return storeIngredient(ingredientObj).orElse(null);
    }
}
