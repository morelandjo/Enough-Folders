package com.enoughfolders.integrations.jei.recipe;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.event.ClientEventHandler;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.client.gui.IngredientSlot;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.jei.core.JEIRuntimeManager;
import com.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler;
import com.enoughfolders.integrations.jei.ingredient.JEIIngredientManager;

import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.Optional;

/**
 * Manages recipe viewing functionality in JEI integration.
 */
public class JEIRecipeManager {

    /**
     * The JEI runtime manager
     */
    private final JEIRuntimeManager runtimeManager;
    
    /**
     * The JEI ingredient manager
     */
    private final JEIIngredientManager ingredientManager;
    
    /**
     * Creates a new JEI recipe manager.
     *
     * @param runtimeManager The JEI runtime manager
     * @param ingredientManager The JEI ingredient manager
     */
    public JEIRecipeManager(JEIRuntimeManager runtimeManager, JEIIngredientManager ingredientManager) {
        this.runtimeManager = runtimeManager;
        this.ingredientManager = ingredientManager;
    }
    
    /**
     * Shows recipes for the provided ingredient in the JEI recipe GUI.
     *
     * @param ingredient The ingredient to show recipes for
     */
    public void showRecipes(Object ingredient) {
        if (!runtimeManager.hasRuntime()) {
            EnoughFolders.LOGGER.error("Cannot show recipes: JEI runtime is not available");
            return;
        }
        
        try {
            saveCurrentFolderScreen();
            
            Optional<IRecipesGui> recipesGuiOpt = runtimeManager.getRecipesGui();
            if (recipesGuiOpt.isPresent()) {
                IRecipesGui recipesGui = recipesGuiOpt.get();
                
                Optional<? extends mezz.jei.api.ingredients.ITypedIngredient<?>> typedIngredient = 
                    runtimeManager.getJeiRuntime()
                        .map(runtime -> runtime.getIngredientManager().createTypedIngredient(ingredient))
                        .orElse(Optional.empty());
                
                if (typedIngredient.isPresent()) {
                    Optional<IFocusFactory> focusFactoryOpt = runtimeManager.getJeiRuntime()
                        .map(runtime -> runtime.getJeiHelpers().getFocusFactory());
                    
                    if (focusFactoryOpt.isPresent()) {
                        IFocusFactory focusFactory = focusFactoryOpt.get();
                        
                        @SuppressWarnings("unchecked")
                        IFocus<?> focus = focusFactory.createFocus(
                            RecipeIngredientRole.OUTPUT,
                            (mezz.jei.api.ingredients.ITypedIngredient) typedIngredient.get()
                        );
                        
                        recipesGui.show(focus);
                        EnoughFolders.LOGGER.debug("Successfully showed recipes for ingredient");
                    }
                } else {
                    EnoughFolders.LOGGER.error("Failed to create typed ingredient for showing recipes");
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error showing recipes for ingredient", e);
        }
    }
    
    /**
     * Shows usages for the provided ingredient in the JEI recipe GUI.
     *
     * @param ingredient The ingredient to show usages for
     */
    public void showUses(Object ingredient) {
        if (!runtimeManager.hasRuntime()) {
            EnoughFolders.LOGGER.error("Cannot show uses: JEI runtime is not available");
            return;
        }
        
        try {
            saveCurrentFolderScreen();
            
            Optional<IRecipesGui> recipesGuiOpt = runtimeManager.getRecipesGui();
            if (recipesGuiOpt.isPresent()) {
                IRecipesGui recipesGui = recipesGuiOpt.get();
                
                Optional<? extends mezz.jei.api.ingredients.ITypedIngredient<?>> typedIngredient = 
                    runtimeManager.getJeiRuntime()
                        .map(runtime -> runtime.getIngredientManager().createTypedIngredient(ingredient))
                        .orElse(Optional.empty());
                
                if (typedIngredient.isPresent()) {
                    Optional<IFocusFactory> focusFactoryOpt = runtimeManager.getJeiRuntime()
                        .map(runtime -> runtime.getJeiHelpers().getFocusFactory());
                    
                    if (focusFactoryOpt.isPresent()) {
                        IFocusFactory focusFactory = focusFactoryOpt.get();
                        
                        @SuppressWarnings("unchecked")
                        IFocus<?> focus = focusFactory.createFocus(
                            RecipeIngredientRole.INPUT,
                            (mezz.jei.api.ingredients.ITypedIngredient) typedIngredient.get()
                        );
                        
                        recipesGui.show(focus);
                        EnoughFolders.LOGGER.debug("Successfully showed uses for ingredient");
                    }
                } else {
                    EnoughFolders.LOGGER.error("Failed to create typed ingredient for showing uses");
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error showing uses for ingredient", e);
        }
    }
    
    /**
     * Saves the current folder screen so it can be displayed on recipe screens.
     */
    private void saveCurrentFolderScreen() {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            ClientEventHandler.getFolderScreen(containerScreen)
                .ifPresent(folderScreen -> {
                    // Force a reinit of the folder screen before saving it
                    int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    folderScreen.init(screenWidth, screenHeight);
                    
                    JEIRecipeGuiHandler.saveLastFolderScreen(folderScreen);
                    EnoughFolders.LOGGER.debug("Saved folder screen for recipe/usage view");
                });
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
            if (!runtimeManager.hasRuntime()) {
                return false;
            }
            
            // Get the stored ingredient from the slot
            StoredIngredient storedIngredient = slot.getIngredient();
            if (storedIngredient == null) {
                return false;
            }
            
            // Convert stored ingredient to JEI ingredient
            Optional<?> ingredientOpt = ingredientManager.getIngredientFromStored(storedIngredient);
            if (ingredientOpt.isEmpty()) {
                return false;
            }
            
            Object ingredient = ingredientOpt.get();
            
            // Determine action based on mouse button and modifiers
            if (button == 0) {
                // Left click - show recipes
                showRecipes(ingredient);
                return true;
            } else if (button == 1) {
                // Right click - show uses
                showUses(ingredient);
                return true;
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error handling ingredient click: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Checks if the JEI recipe GUI is currently open.
     *
     * @return true if the JEI recipe GUI is the current screen
     */
    public boolean isRecipeGuiOpen() {
        if (!runtimeManager.hasRuntime()) {
            return false;
        }
        
        try {
            Optional<IRecipesGui> recipesGuiOpt = runtimeManager.getRecipesGui();
            if (recipesGuiOpt.isEmpty()) {
                return false;
            }
            
            // Check if the recipe GUI is the current screen
            Screen currentScreen = Minecraft.getInstance().screen;
            return currentScreen != null && currentScreen.equals(recipesGuiOpt.get());
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error checking if JEI recipe GUI is open", e);
            return false;
        }
    }
    
    /**
     * Check if the given screen is a recipe screen for this integration.
     * 
     * @param screen The screen to check
     * @return True if it's a recipe screen for this integration, false otherwise
     */
    public boolean isRecipeScreen(Screen screen) {
        if (screen == null || !runtimeManager.hasRuntime()) {
            return false;
        }
        
        try {
            // Check if the screen is a JEI IRecipesGui
            Class<?> recipesGuiClass = Class.forName("mezz.jei.api.runtime.IRecipesGui");
            return recipesGuiClass.isInstance(screen);
        } catch (ClassNotFoundException e) {
            // JEI is not installed or class not found
            return false;
        }
    }
    
    /**
     * Check if the screen being closed is transitioning to a recipe screen for this integration.
     * 
     * @param screen The screen that's being closed
     * @return True if we're transitioning to a recipe screen, false otherwise
     */
    public boolean isTransitioningToRecipeScreen(Screen screen) {
        if (!runtimeManager.hasRuntime()) {
            return false;
        }
        
        try {
            // We need to check if this closure is due to JEI opening a recipe view
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                // Check if JEI recipe classes are in the stack trace
                if (element.getClassName().contains("mezz.jei") && 
                    (element.getMethodName().contains("show") || 
                     element.getClassName().contains("RecipesGui"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.debug("Error checking for JEI recipe transition: {}", e.getMessage());
        }
        
        return false;
    }
}
