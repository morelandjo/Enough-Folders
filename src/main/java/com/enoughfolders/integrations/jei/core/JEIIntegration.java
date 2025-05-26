package com.enoughfolders.integrations.jei.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.client.gui.IngredientSlot;

import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.ModIntegration;
import com.enoughfolders.integrations.api.FolderTarget;
import com.enoughfolders.integrations.api.RecipeViewingIntegration;

import com.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler;
import com.enoughfolders.integrations.jei.ingredient.JEIIngredientManager;
import com.enoughfolders.integrations.jei.recipe.JEIRecipeManager;
import com.enoughfolders.integrations.util.StackTraceUtils;

import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.List;
import java.util.Optional;

/**
 * Integration with JEI mod.
 */
public class JEIIntegration implements ModIntegration, RecipeViewingIntegration {
    
    /**
     * JEI mod identifier
     */
    private static final String MOD_ID = "jei";
    
    /**
     * The JEI runtime manager
     */
    private final JEIRuntimeManager runtimeManager;
    
    /**
     * Manager for JEI ingredients
     */
    private final JEIIngredientManager ingredientManager;
    
    /**
     * Manager for JEI recipe operations
     */
    private final JEIRecipeManager recipeManager;
    
    /**
     * Creates a new JEI integration.
     */
    public JEIIntegration() {
        this.runtimeManager = new JEIRuntimeManager();
        this.ingredientManager = new JEIIngredientManager(this.runtimeManager);
        this.recipeManager = new JEIRecipeManager(this.runtimeManager, this.ingredientManager);
        
        EnoughFolders.LOGGER.info("JEI Integration initialized");
    }
    
    /**
     * Gets the display name of this mod integration.
     *
     * @return The display name of the integration
     */
    @Override
    public String getModName() {
        return "JEI";
    }
    
    /**
     * Checks if JEI is available in the current environment.
     *
     * @return true if JEI mod is loaded, false otherwise
     */
    @Override
    public boolean isAvailable() {
        return ModList.get().isLoaded(MOD_ID);
    }
    
    /**
     * Sets the JEI runtime reference.
     *
     * @param jeiRuntime The JEI runtime instance
     */
    public void setJeiRuntime(IJeiRuntime jeiRuntime) {
        this.runtimeManager.setJeiRuntime(jeiRuntime);
        EnoughFolders.LOGGER.info("JEI Runtime available, integration active");
    }
    
    /**
     * Gets the JEI runtime reference.
     *
     * @return Optional containing the JEI runtime, or empty if not available
     */
    public Optional<IJeiRuntime> getJeiRuntime() {
        return runtimeManager.getJeiRuntime();
    }
    
    /**
     * Converts a StoredIngredient back to its original JEI ingredient object.
     *
     * @param storedIngredient The stored ingredient to convert
     * @return Optional containing the original ingredient object, or empty if conversion failed
     */
    @Override
    public Optional<?> getIngredientFromStored(StoredIngredient storedIngredient) {
        return ingredientManager.getIngredientFromStored(storedIngredient);
    }
    
    /**
     * Converts a JEI ingredient object into a StoredIngredient for persistence.
     *
     * @param ingredient The JEI ingredient object to convert
     * @return Optional containing the StoredIngredient, or empty if conversion failed
     */
    @Override
    public Optional<StoredIngredient> storeIngredient(Object ingredient) {
        return ingredientManager.storeIngredient(ingredient);
    }
    
    /**
     * Gets an ItemStack that can be used to visually represent the ingredient.
     *
     * @param ingredient The ingredient to get an ItemStack for
     * @return Optional containing the ItemStack, or empty if conversion failed
     */
    @Override
    public Optional<ItemStack> getItemStackForDisplay(Object ingredient) {
        return ingredientManager.getItemStackForDisplay(ingredient);
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
    public void renderIngredient(GuiGraphics graphics, StoredIngredient ingredient, int x, int y, int width, int height) {
        ingredientManager.renderIngredient(graphics, ingredient, x, y, width, height);
    }
    
    /**
     * Shows recipes for the provided ingredient in the JEI recipe GUI.
     *
     * @param ingredient The ingredient to show recipes for
     */
    public void showRecipes(Object ingredient) {
        recipeManager.showRecipes(ingredient);
    }
    
    /**
     * Shows usages for the provided ingredient in the JEI recipe GUI.
     *
     * @param ingredient The ingredient to show usages for
     */
    public void showUses(Object ingredient) {
        recipeManager.showUses(ingredient);
    }
    
    /**
     * Initializes the JEI integration.
     */
    @Override
    public void initialize() {
        EnoughFolders.LOGGER.info("Initializing JEI integration");
    }

    /**
     * Checks if the JEI recipe GUI is currently open.
     *
     * @return true if the JEI recipe GUI is the current screen
     */
    public boolean isRecipeGuiOpen() {
        return recipeManager.isRecipeGuiOpen();
    }
    
    /**
     * Check if the given screen is a recipe screen for this integration.
     * 
     * @param screen The screen to check
     * @return True if it's a recipe screen for this integration, false otherwise
     */
    @Override
    public boolean isRecipeScreen(Screen screen) {
        return recipeManager.isRecipeScreen(screen);
    }
    
    /**
     * Gets the display name of the integration.
     * 
     * @return The display name
     */
    @Override
    public String getDisplayName() {
        return "JEI";
    }
    
    /**
     * Connect a folder screen to JEI for recipe viewing.
     * 
     * @param folderScreen The folder screen to connect
     * @param containerScreen The container screen
     */
    @Override
    public void connectToFolderScreen(FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen) {
        try {
            // Create handler if available
            if (isAvailable()) {
                // Connect the folder screen to JEI functionality
                folderScreen.registerIngredientClickHandler((slot, button, shift, ctrl) -> 
                    handleIngredientClick(slot, button, shift, ctrl));
                
                EnoughFolders.LOGGER.debug("Connected folder screen to JEI");
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.debug("Could not connect folder to JEI: {}", e.getMessage());
        }
    }
    
    /**
     * Save a folder screen to be used during recipe GUI navigation.
     * 
     * @param folderScreen The folder screen to save
     */
    @Override
    public void saveLastFolderScreen(FolderScreen folderScreen) {
        JEIRecipeGuiHandler.saveLastFolderScreen(folderScreen);
    }
    
    /**
     * Clear the saved folder screen when no longer needed.
     */
    @Override
    public void clearLastFolderScreen() {
        JEIRecipeGuiHandler.clearLastFolderScreen();
    }
    
    /**
     * Get the last folder screen saved for recipe GUI navigation.
     * 
     * @return Optional containing the folder screen if available
     */
    @Override
    public Optional<FolderScreen> getLastFolderScreen() {
        return JEIRecipeGuiHandler.getLastFolderScreen();
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
            // Using centralized stack trace utility
            return StackTraceUtils.isJEIRecipeTransition();
        } catch (Exception e) {
            EnoughFolders.LOGGER.debug("Error checking for JEI recipe transition: {}", e.getMessage());
        }
        
        return false;
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
    public boolean handleIngredientClick(IngredientSlot slot, int button, 
                                          boolean shift, boolean ctrl) {
        return recipeManager.handleIngredientClick(slot, button, shift, ctrl);
    }
    
    /**
     * Creates folder targets for drag and drop operations.
     * Returns empty list as drag and drop functionality has been removed.
     * 
     * @param folderButtons The list of folder buttons
     * @return Empty list - drag and drop is disabled
     */
    @Override
    public List<? extends FolderTarget> createFolderTargets(List<FolderButton> folderButtons) {
        // Return empty list as drag and drop functionality has been removed
        return List.of();
    }

}
