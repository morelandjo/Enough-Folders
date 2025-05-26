package com.enoughfolders.integrations.emi.core;

import com.enoughfolders.integrations.base.AbstractRecipeManager;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

/**
 * Manages recipe viewing operations for EMI integration.
 */
public class EMIRecipeManager extends AbstractRecipeManager {
    
    /**
     * Creates a new EMI recipe manager.
     */
    public EMIRecipeManager() {
        super("EMI");
    }

    /**
     * Performs the actual recipe showing logic.
     *
     * @param ingredient The ingredient to show recipes for
     */
    @Override
    protected void doShowRecipes(Object ingredient) {
        try {
            // Use EMI's API to show recipes
            Class<?> emiApiClass = Class.forName("dev.emi.emi.api.EmiApi");
            Class<?> emiIngredientClass = Class.forName("dev.emi.emi.api.stack.EmiIngredient");
            
            // Convert the ingredient to EmiIngredient if needed
            Object emiIngredient = convertToEmiIngredient(ingredient);
            if (emiIngredient == null) {
                return;
            }
            
            java.lang.reflect.Method displayRecipesMethod = emiApiClass.getMethod("displayRecipes", emiIngredientClass);
            displayRecipesMethod.invoke(null, emiIngredient);
            
        } catch (Exception e) {
            // Error handled by base class
        }
    }

    /**
     * Performs the actual uses showing logic.
     *
     * @param ingredient The ingredient to show uses for
     */
    @Override
    protected void doShowUses(Object ingredient) {
        try {
            // Use EMI's API to show uses
            Class<?> emiApiClass = Class.forName("dev.emi.emi.api.EmiApi");
            Class<?> emiIngredientClass = Class.forName("dev.emi.emi.api.stack.EmiIngredient");
            
            // Convert the ingredient to EmiIngredient if needed
            Object emiIngredient = convertToEmiIngredient(ingredient);
            if (emiIngredient == null) {
                return;
            }
            
            java.lang.reflect.Method displayUsesMethod = emiApiClass.getMethod("displayUses", emiIngredientClass);
            displayUsesMethod.invoke(null, emiIngredient);
            
        } catch (Exception e) {
            // Error handled by base class
        }
    }

    /**
     * Check if the given screen is an EMI recipe screen.
     * @param screen The screen to check
     * @return true if the screen is an EMI recipe screen, false otherwise
     */
    public boolean isEMIRecipeScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        
        try {
            String className = screen.getClass().getName();
            // Check for EMI recipe screen classes
            return className.contains("dev.emi.emi") && 
                   (className.contains("recipe") || className.contains("Recipe"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the current hovered ingredient from EMI.
     * @return The currently hovered ingredient, or null if none
     */
    public Object getHoveredIngredient() {
        try {
            // Use EMI's API to get hovered ingredient
            Class<?> emiApiClass = Class.forName("dev.emi.emi.api.EmiApi");
            java.lang.reflect.Method getHoveredStackMethod = emiApiClass.getMethod("getHoveredStack");
            return getHoveredStackMethod.invoke(null);
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Convert an object to an EmiIngredient if possible.
     */
    private Object convertToEmiIngredient(Object ingredient) {
        if (ingredient == null) {
            return null;
        }
        
        try {
            Class<?> emiStackClass = Class.forName("dev.emi.emi.api.stack.EmiStack");
            Class<?> emiIngredientClass = Class.forName("dev.emi.emi.api.stack.EmiIngredient");
            
            // If it's already an EmiIngredient, return it
            if (emiIngredientClass.isInstance(ingredient)) {
                return ingredient;
            }
            
            // If it's an EmiStack, it implements EmiIngredient, so return it
            if (emiStackClass.isInstance(ingredient)) {
                return ingredient;
            }
            
            // If it's an ItemStack, convert it to EmiStack
            if (ingredient instanceof ItemStack) {
                ItemStack itemStack = (ItemStack) ingredient;
                java.lang.reflect.Method ofMethod = emiStackClass.getMethod("of", ItemStack.class);
                return ofMethod.invoke(null, itemStack);
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Save a folder screen to be used during recipe GUI navigation.
     * 
     * @param folderScreen The folder screen to save
     */
    public void saveLastFolderScreen(com.enoughfolders.client.gui.FolderScreen folderScreen) {
        // EMI doesn't need folder screen persistence like JEI, so this is a no-op
    }
    
    /**
     * Clear the saved folder screen when no longer needed.
     */
    public void clearLastFolderScreen() {
        // EMI doesn't need folder screen persistence like JEI, so this is a no-op
    }
    
    /**
     * Get the last folder screen saved for recipe GUI navigation.
     * 
     * @return Optional containing the folder screen if available
     */
    public java.util.Optional<com.enoughfolders.client.gui.FolderScreen> getLastFolderScreen() {
        // EMI doesn't persist folder screens like JEI, so return empty
        return java.util.Optional.empty();
    }
    
    /**
     * Check if the given screen is a recipe screen for this integration.
     * 
     * @param screen The screen to check
     * @return True if it's a recipe screen for this integration, false otherwise
     */
    public static boolean isRecipeScreen(Screen screen) {
        try {
            // Check if the screen is an EMI recipe screen
            return screen.getClass().getName().contains("emi") && 
                   screen.getClass().getName().toLowerCase().contains("recipe");
        } catch (Exception e) {
            return false;
        }
    }
}
