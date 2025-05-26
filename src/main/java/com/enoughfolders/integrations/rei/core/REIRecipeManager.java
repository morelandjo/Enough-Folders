package com.enoughfolders.integrations.rei.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.integrations.base.AbstractRecipeManager;
import com.enoughfolders.client.gui.IngredientSlot;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.data.StoredIngredient;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Manages recipe viewing operations for REI integration.
 */
public class REIRecipeManager extends AbstractRecipeManager {
    
    /**
     * The REI ingredient manager
     */
    private final REIIngredientManager ingredientManager;
    
    /**
     * The saved folder screen for navigation back from recipe GUI
     */
    private FolderScreen lastFolderScreen;
    
    /**
     * Creates a new REI recipe manager.
     * 
     * @param ingredientManager The REI ingredient manager
     */
    public REIRecipeManager(REIIngredientManager ingredientManager) {
        super("REI");
        this.ingredientManager = ingredientManager;
    }
    
    /**
     * Performs the actual recipe showing logic.
     *
     * @param ingredient The ingredient to show recipes for
     */
    @Override
    protected void doShowRecipes(Object ingredient) {
        try {
            // Use REI's API to show recipes
            Class<?> reiApiClass = Class.forName("me.shedaniel.rei.api.client.ClientHelper");
            Class<?> entryStackClass = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");
            
            // Convert the ingredient to REI EntryStack if needed
            Object reiIngredient = convertToREIIngredient(ingredient);
            if (reiIngredient == null) {
                return;
            }
            
            java.lang.reflect.Method getInstanceMethod = reiApiClass.getMethod("getInstance");
            Object clientHelper = getInstanceMethod.invoke(null);
            
            java.lang.reflect.Method openRecipesMethod = clientHelper.getClass().getMethod("openRecipeScreen", entryStackClass);
            openRecipesMethod.invoke(clientHelper, reiIngredient);
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to show recipes in REI", e);
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
            // Use REI's API to show uses
            Class<?> reiApiClass = Class.forName("me.shedaniel.rei.api.client.ClientHelper");
            Class<?> entryStackClass = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");
            
            // Convert the ingredient to REI EntryStack if needed
            Object reiIngredient = convertToREIIngredient(ingredient);
            if (reiIngredient == null) {
                return;
            }
            
            java.lang.reflect.Method getInstanceMethod = reiApiClass.getMethod("getInstance");
            Object clientHelper = getInstanceMethod.invoke(null);
            
            java.lang.reflect.Method openUsesMethod = clientHelper.getClass().getMethod("openUsageScreen", entryStackClass);
            openUsesMethod.invoke(clientHelper, reiIngredient);
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to show uses in REI", e);
        }
    }
    
    /**
     * Convert an object to a REI EntryStack if possible.
     * 
     * @param ingredient The ingredient to convert
     * @return The REI EntryStack, or null if conversion failed
     */
    private Object convertToREIIngredient(Object ingredient) {
        if (ingredient == null) {
            return null;
        }
        
        try {
            Class<?> entryStackClass = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");
            Class<?> vanillaTypesClass = Class.forName("me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes");
            
            // If it's already an EntryStack, return it
            if (entryStackClass.isInstance(ingredient)) {
                return ingredient;
            }
            
            // If it's an ItemStack, convert it
            if (ingredient instanceof ItemStack) {
                ItemStack itemStack = (ItemStack) ingredient;
                
                java.lang.reflect.Field itemField = vanillaTypesClass.getField("ITEM");
                Object itemType = itemField.get(null);
                
                java.lang.reflect.Method ofMethod = entryStackClass.getMethod("of", Object.class, Object.class);
                return ofMethod.invoke(null, itemType, itemStack);
            }
            
            return null;
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to convert to REI EntryStack", e);
            return null;
        }
    }
    
    /**
     * Check if the given screen is a recipe screen for this integration.
     * 
     * @param screen The screen to check
     * @return True if it's a recipe screen for this integration, false otherwise
     */
    public boolean isRecipeScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        
        try {
            String className = screen.getClass().getName();
            // Check for REI recipe screen classes
            return className.contains("me.shedaniel.rei") && 
                   (className.contains("recipe") || className.contains("Recipe"));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Handle a click on an ingredient slot in a folder.
     *
     * @param slot The ingredient slot that was clicked
     * @param button The mouse button used (0 = left, 1 = right)
     * @param shift Whether shift was held
     * @param ctrl Whether ctrl was held
     * @return true if the click was handled, false otherwise
     */
    public boolean handleIngredientClick(IngredientSlot slot, int button, boolean shift, boolean ctrl) {
        try {
            // Get the stored ingredient from the slot
            StoredIngredient storedIngredient = slot.getIngredient();
            if (storedIngredient == null) {
                return false;
            }
            
            // Convert stored ingredient to REI ingredient
            Optional<?> ingredientOpt = ingredientManager.getIngredientFromStored(storedIngredient);
            if (ingredientOpt.isEmpty()) {
                return false;
            }
            
            Object reiIngredient = convertToREIIngredient(ingredientOpt.get());
            if (reiIngredient == null) {
                return false;
            }
            
            // Determine action based on mouse button and modifiers
            if (button == 0) {
                // Left click - show recipes
                showRecipes(reiIngredient);
                return true;
            } else if (button == 1) {
                // Right click - show uses
                showUses(reiIngredient);
                return true;
            }
        } catch (Exception e) {
            logError("Error handling ingredient click: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Save a folder screen to be used during recipe GUI navigation.
     * 
     * @param folderScreen The folder screen to save
     */
    public void saveLastFolderScreen(FolderScreen folderScreen) {
        this.lastFolderScreen = folderScreen;
        logDebug("Saved folder screen for REI recipe navigation");
    }
    
    /**
     * Clear the saved folder screen when no longer needed.
     */
    public void clearLastFolderScreen() {
        this.lastFolderScreen = null;
        logDebug("Cleared last folder screen for REI");
    }
    
    /**
     * Get the last folder screen saved for recipe GUI navigation.
     * 
     * @return Optional containing the folder screen, or empty if none saved
     */
    public Optional<FolderScreen> getLastFolderScreen() {
        return Optional.ofNullable(lastFolderScreen);
    }
    
    /**
     * Checks if the REI recipe GUI is currently open.
     *
     * @return true if the REI recipe GUI is the current screen
     */
    @Override
    public boolean isRecipeGuiOpen() {
        try {
            // Get current screen and check if it's a REI recipe screen
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            Screen currentScreen = mc.screen;
            return isRecipeScreen(currentScreen);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets the current screen if it's a recipe GUI screen.
     *
     * @return Optional containing the recipe screen, or empty if not a recipe screen
     */
    @Override
    public Optional<Screen> getCurrentRecipeScreen() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            Screen currentScreen = mc.screen;
            if (isRecipeScreen(currentScreen)) {
                return Optional.of(currentScreen);
            }
        } catch (Exception e) {
            // Ignore and return empty
        }
        return Optional.empty();
    }
    
    /**
     * Get the current hovered ingredient from REI.
     * 
     * @return The currently hovered ingredient, or null if none
     */
    public Object getHoveredIngredient() {
        try {
            Class<?> reiRuntimeClass = Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            
            java.lang.reflect.Method getInstanceMethod = reiRuntimeClass.getMethod("getInstance");
            Object runtime = getInstanceMethod.invoke(null);
            
            if (runtime != null) {
                java.lang.reflect.Method getHoveredStackMethod = runtime.getClass().getMethod("getHoveredStack");
                return getHoveredStackMethod.invoke(runtime);
            }
            
            return null;
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to get hovered ingredient from REI", e);
            return null;
        }
    }
}
