package com.enoughfolders.integrations.emi.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.base.AbstractIntegration;
import com.enoughfolders.integrations.emi.gui.handlers.EMIFolderIngredientHandler;
import com.enoughfolders.integrations.factory.HandlerFactory;
import com.enoughfolders.integrations.factory.IntegrationFactory;
import com.enoughfolders.integrations.util.StackTraceUtils;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.Optional;

/**
 * Integration for EMI (Enough Modding Interface) mod.
 */
public class EMIIntegration extends AbstractIntegration {
    
    /**
     * EMI mod identifier
     */
    private static final String MOD_ID = "emi";
    
    /**
     * EMI ingredient manager instance
     */
    private EMIIngredientManager ingredientManager;
    
    /**
     * EMI recipe manager instance
     */
    private EMIRecipeManager recipeManager;
    
    /**
     * Creates a new EMI integration instance.
     */
    public EMIIntegration() {
        super(MOD_ID, "EMI");
        this.ingredientManager = new EMIIngredientManager();
        this.recipeManager = new EMIRecipeManager();
        EnoughFolders.LOGGER.info("EMI Integration initialized");
        DebugLogger.debugValues(
            DebugLogger.Category.INTEGRATION, 
            "EMI Integration created - instance: {}, recipe manager instance: {}", 
            System.identityHashCode(this), 
            System.identityHashCode(this.recipeManager));
        DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
            "EMI Integration created - instance: {}, recipe manager instance: {}", 
            System.identityHashCode(this), 
            System.identityHashCode(this.recipeManager));
    }
    
    /**
     * Checks if the required EMI classes are available.
     *
     * @return true if EMI classes are available, false otherwise
     */
    @Override
    protected boolean checkClassAvailability() {
        try {
            Class.forName("dev.emi.emi.api.EmiApi");
            Class.forName("dev.emi.emi.api.stack.EmiStack");
            Class.forName("dev.emi.emi.api.stack.EmiIngredient");
            return ModList.get().isLoaded(MOD_ID);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Performs EMI-specific initialization.
     */
    @Override
    protected void doInitialize() {
        try {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Initializing EMI integration", ""
            );
            
            // Register handler factories for all integrations
            com.enoughfolders.integrations.factory.HandlerFactory.registerDefaultFactories();
            
            // Register EMI plugin
            EMIPlugin.register();
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI integration initialization complete", ""
            );
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to initialize EMI integration", e);
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI integration initialization failed: {}", 
                e.getMessage()
            );
            throw e;
        }
    }
    
    /**
     * Gets the display name of this mod integration.
     *
     * @return The display name of the integration
     */
    @Override
    public String getModName() {
        return "EMI";
    }
    
    /**
     * Converts a StoredIngredient back to its original EMI ingredient object.
     *
     * @param storedIngredient The stored ingredient to convert
     * @return Optional containing the original ingredient object, or empty if conversion failed
     */
    @Override
    public Optional<?> getIngredientFromStored(StoredIngredient storedIngredient) {
        try {
            return ingredientManager.getIngredientFromStored(storedIngredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error converting StoredIngredient to EMI ingredient: {}", 
                e.getMessage()
            );
            return Optional.empty();
        }
    }
    
    /**
     * Converts an EMI ingredient object into a StoredIngredient for persistence.
     *
     * @param ingredient The EMI ingredient object to convert
     * @return Optional containing the StoredIngredient, or empty if conversion failed
     */
    @Override
    public Optional<StoredIngredient> storeIngredient(Object ingredient) {
        try {
            return ingredientManager.storeIngredient(ingredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error converting EMI ingredient to StoredIngredient: {}", 
                e.getMessage()
            );
            return Optional.empty();
        }
    }
    
    /**
     * Gets an ItemStack that can be used to visually represent the ingredient.
     *
     * @param ingredient The ingredient to get an ItemStack for
     * @return Optional containing the ItemStack, or empty if conversion failed
     */
    @Override
    public Optional<ItemStack> getItemStackForDisplay(Object ingredient) {
        try {
            return ingredientManager.getItemStackForDisplay(ingredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error getting ItemStack for EMI ingredient: {}", 
                e.getMessage()
            );
            return Optional.empty();
        }
    }
     /**
     * Gets the ingredient currently under the mouse cursor in EMI.
     * 
     * @return Optional containing the ingredient under mouse, or empty if none
     */
    public Optional<Object> getIngredientUnderMouse() {
        if (!isAvailable()) {
            return Optional.empty();
        }
        
        try {
            Object hoveredIngredient = recipeManager.getHoveredIngredient();
            return Optional.ofNullable(hoveredIngredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error getting ingredient under mouse: {}",
                e.getMessage()
            );
            return Optional.empty();
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
    public void renderIngredient(GuiGraphics graphics, StoredIngredient ingredient, int x, int y, int width, int height) {
        try {
            // Try to convert the stored ingredient back to an EMI ingredient
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
                
                DebugLogger.debug(DebugLogger.Category.INTEGRATION, 
                    "Successfully rendered EMI ingredient at " + x + "," + y);
            } else {
                DebugLogger.debug(DebugLogger.Category.INTEGRATION, 
                    "Failed to get ItemStack for rendering EMI ingredient");
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to render EMI ingredient", e);
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Exception details: {}", e.getMessage());
        }
    }
    
    /**
     * Shows recipes for the provided ingredient in the EMI recipe GUI.
     *
     * @param ingredient The ingredient to show recipes for
     */
    public void showRecipes(Object ingredient) {
        try {
            recipeManager.showRecipes(ingredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error showing EMI recipes: {}", 
                e.getMessage()
            );
        }
    }
    
    /**
     * Shows usages for the provided ingredient in the EMI recipe GUI.
     *
     * @param ingredient The ingredient to show usages for
     */
    public void showUses(Object ingredient) {
        try {
            recipeManager.showUses(ingredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error showing EMI uses: {}", 
                e.getMessage()
            );
        }
    }
    
    /**
     * Connect a folder screen to EMI for recipe viewing.
     * 
     * @param folderScreen The folder screen to connect
     * @param containerScreen The container screen
     */
    @Override
    public void connectToFolderScreen(FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen) {
        try {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Connecting EMI to folder screen", ""
            );
            
            // Create handlers for this folder screen using factory
            try {
                Object folderHandler = HandlerFactory.createHandler(
                    IntegrationFactory.IntegrationType.EMI,
                    HandlerFactory.HandlerType.FOLDER_SCREEN,
                    Object.class
                );
                DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                    "EMI folder screen handler created via factory: {}", folderHandler.getClass().getSimpleName());
            } catch (Exception factoryException) {
                DebugLogger.debugValue(DebugLogger.Category.INTEGRATION,
                    "Factory creation failed, falling back to direct instantiation: {}", factoryException.getMessage());
                // Fallback to direct instantiation
                new EMIFolderIngredientHandler(folderScreen, containerScreen);
            }
            
            // Store the folder screen for later use
            saveLastFolderScreen(folderScreen);
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI folder screen connection complete", ""
            );
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error connecting EMI to folder screen: {}", 
                e.getMessage()
            );
        }
    }
    
    /**
     * Save a folder screen to be used during recipe GUI navigation.
     * 
     * @param folderScreen The folder screen to save
     */
    @Override
    public void saveLastFolderScreen(FolderScreen folderScreen) {
        try {
            recipeManager.saveLastFolderScreen(folderScreen);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error saving EMI folder screen: {}", 
                e.getMessage()
            );
        }
    }
    
    /**
     * Clear the saved folder screen when no longer needed.
     */
    @Override
    public void clearLastFolderScreen() {
        try {
            DebugLogger.debugValues(
                DebugLogger.Category.INTEGRATION, 
                "EMI Integration clearLastFolderScreen called - instance: {}, recipe manager instance: {}", 
                System.identityHashCode(this), 
                System.identityHashCode(this.recipeManager)
            );
            recipeManager.clearLastFolderScreen();
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error clearing EMI folder screen: {}", 
                e.getMessage()
            );
        }
    }
    
    /**
     * Get the last folder screen saved for recipe GUI navigation.
     * 
     * @return Optional containing the folder screen if available
     */
    @Override
    public Optional<FolderScreen> getLastFolderScreen() {
        try {
            DebugLogger.debugValues(
                DebugLogger.Category.INTEGRATION, 
                "EMI Integration getLastFolderScreen called - instance: {}, recipe manager instance: {}", 
                System.identityHashCode(this), 
                System.identityHashCode(this.recipeManager)
            );
            return recipeManager.getLastFolderScreen();
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error getting EMI folder screen: {}", 
                e.getMessage()
            );
            return Optional.empty();
        }
    }
    
    /**
     * Check if the given screen is a recipe screen for this integration.
     * 
     * @param screen The screen to check
     * @return True if it's a recipe screen for this integration, false otherwise
     */
    @Override
    public boolean isRecipeScreen(Screen screen) {
        try {
            return EMIRecipeManager.isRecipeScreen(screen);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error checking EMI recipe screen: {}", 
                e.getMessage()
            );
            return false;
        }
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
            return StackTraceUtils.isEMIRecipeTransition();
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error checking EMI recipe transition: {}", 
                e.getMessage()
            );
            return false;
        }
    }
    
    /**
     * Checks if EMI mod is loaded and available.
     * 
     * @return true if EMI is loaded, false otherwise
     */
    public static boolean isEMILoaded() {
        try {
            Class.forName("dev.emi.emi.api.EmiApi");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Gets the installed version of EMI.
     * 
     * @return the EMI version string, or "unknown" if EMI is not loaded
     */
    public static String getEMIVersion() {
        if (!isEMILoaded()) {
            return "Not loaded";
        }
            return "Available";
    }
    
    /**
     * Gets the EMI ingredient manager instance.
     *
     * @return The EMI ingredient manager
     */
    public EMIIngredientManager getIngredientManager() {
        return ingredientManager;
    }
    
    /**
     * Gets the EMI recipe manager instance.
     *
     * @return The EMI recipe manager
     */
    public EMIRecipeManager getRecipeManager() {
        return recipeManager;
    }
}
