package com.enoughfolders.client.gui;

import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.IntegrationRegistry;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Represents a slot for displaying an ingredient in the active folder's view.
 */
public class IngredientSlot {
    /**
     * Position and content of this slot
     */
    private final int x;
    private final int y;
    private final StoredIngredient ingredient;
    private static final int SIZE = 18;
    
    /**
     * Creates a new ingredient slot.
     *
     * @param x The x position of the slot
     * @param y The y position of the slot
     * @param ingredient The ingredient to display in this slot
     */
    public IngredientSlot(int x, int y, StoredIngredient ingredient) {
        this.x = x;
        this.y = y;
        this.ingredient = ingredient;
    }
    
    /**
     * Renders this ingredient slot and its contents.
     *
     * @param graphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean isHovered = isHovered(mouseX, mouseY);
        
        if (isHovered) {
            graphics.fill(x, y, x + SIZE, y + SIZE, 0x80FFFFFF);
        }
        
        IntegrationRegistry.renderIngredient(graphics, ingredient, x + 1, y + 1, SIZE - 2, SIZE - 2);
    }
    
    /**
     * Handles mouse clicks on this ingredient slot.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param button The mouse button that was clicked
     * @return true if the click was handled, false otherwise
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) {
            return false;
        }
        
        if (button == 0) {
            showRecipes();
            return true;
        } else if (button == 1) {
            showUses();
            return true;
        }
        
        return false;
    }
    
    /**
     * Shows recipes for this ingredient in the available recipe mod.
     */
    private void showRecipes() {
        com.enoughfolders.integrations.RecipeIntegrationHelper.showRecipes(ingredient);
    }
    
    /**
     * Shows uses for this ingredient in the available recipe mod.
     */
    private void showUses() {
        com.enoughfolders.integrations.RecipeIntegrationHelper.showUses(ingredient);
    }
    
    /**
     * Checks if the mouse is hovering over this ingredient slot.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @return true if the mouse is hovering over this slot, false otherwise
     */
    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + SIZE &&
               mouseY >= y && mouseY < y + SIZE;
    }
    
    /**
     * Gets the ingredient displayed in this slot.
     *
     * @return The stored ingredient
     */
    public StoredIngredient getIngredient() {
        return ingredient;
    }
    
    // Required getters for REI integration

    /**
     * Gets the x position of this slot.
     * @return The x position
     */
    public int getX() {
        return x;
    }
    
    /**
     * Gets the y position of this slot.
     * @return The y position
     */
    public int getY() {
        return y;
    }
    
    /**
     * Gets the width of this slot.
     * @return The width
     */
    public int getWidth() {
        return SIZE;
    }
    
    /**
     * Gets the height of this slot.
     * @return The height
     */
    public int getHeight() {
        return SIZE;
    }
    
    /**
     * Checks if this slot has an ingredient.
     * @return true if this slot has an ingredient, false otherwise
     */
    public boolean hasIngredient() {
        return ingredient != null;
    }
    
    /**
     * Gets the stored ingredient in this slot.
     * @return The stored ingredient
     */
    public StoredIngredient getStoredIngredient() {
        return ingredient;
    }
}
