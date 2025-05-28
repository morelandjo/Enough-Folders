package com.enoughfolders.integrations.rei.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.client.gui.IngredientSlot;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.base.AbstractIntegration;
import com.enoughfolders.integrations.common.handlers.BaseRecipeGuiHandler;
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
 * Integration with Roughly Enough Items (REI) mod.
 */
public class REIIntegration extends AbstractIntegration {
    
    /**
     * REI mod identifier
     */
    private static final String MOD_ID = "roughlyenoughitems";
    
    /**
     * REI ingredient manager instance
     */
    private REIIngredientManager ingredientManager;
    
    /**
     * REI recipe manager instance
     */
    private REIRecipeManager recipeManager;
    
    /**
     * Creates a new REI integration instance.
     */
    public REIIntegration() {
        super(MOD_ID, "REI");
        this.ingredientManager = new REIIngredientManager();
        this.recipeManager = new REIRecipeManager(this.ingredientManager);
        EnoughFolders.LOGGER.info("REI Integration initialized");
    }
    
    /**
     * Checks if the required REI classes are available.
     *
     * @return true if REI classes are available, false otherwise
     */
    @Override
    protected boolean checkClassAvailability() {
        try {
            Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");
            Class.forName("me.shedaniel.rei.api.client.ClientHelper");
            return ModList.get().isLoaded(MOD_ID);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Performs REI-specific initialization.
     */
    @Override
    protected void doInitialize() {
        try {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Initializing REI integration", ""
            );
            
            // Register handler factories for all integrations
            com.enoughfolders.integrations.factory.HandlerFactory.registerDefaultFactories();
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "REI integration initialization complete", ""
            );
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to initialize REI integration", e);
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "REI integration initialization failed: {}", 
                e.getMessage()
            );
            throw e;
        }
    }
    
    /**
     * Gets the REI ingredient manager instance.
     *
     * @return The REI ingredient manager
     */
    public REIIngredientManager getIngredientManager() {
        return ingredientManager;
    }
    
    /**
     * Gets the REI recipe manager instance.
     *
     * @return The REI recipe manager
     */
    public REIRecipeManager getRecipeManager() {
        return recipeManager;
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
     * Converts a StoredIngredient back to its original REI ingredient object.
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
                "Error converting StoredIngredient to REI ingredient: {}", 
                e.getMessage()
            );
            return Optional.empty();
        }
    }
    
    /**
     * Converts a REI ingredient object into a StoredIngredient for persistence.
     *
     * @param ingredient The REI ingredient object to convert
     * @return Optional containing the StoredIngredient, or empty if conversion failed
     */
    @Override
    public Optional<StoredIngredient> storeIngredient(Object ingredient) {
        try {
            return ingredientManager.storeIngredient(ingredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error converting REI ingredient to StoredIngredient: {}", 
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
                "Error getting ItemStack for REI ingredient: {}", 
                e.getMessage()
            );
            return Optional.empty();
        }
    }
    
    /**
     * Gets the ingredient currently under the mouse cursor in REI.
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
     * Shows recipes for the provided ingredient in the REI recipe GUI.
     *
     * @param ingredient The ingredient to show recipes for
     */
    public void showRecipes(Object ingredient) {
        try {
            recipeManager.showRecipes(ingredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error showing REI recipes: {}", 
                e.getMessage()
            );
        }
    }
    
    /**
     * Shows usages for the provided ingredient in the REI recipe GUI.
     *
     * @param ingredient The ingredient to show usages for
     */
    public void showUses(Object ingredient) {
        try {
            recipeManager.showUses(ingredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error showing REI uses: {}", 
                e.getMessage()
            );
        }
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
            if (isAvailable()) {
                // Try to create REI folder ingredient handler using factory
                try {
                    Object folderIngredientHandler = HandlerFactory.createHandler(
                        IntegrationFactory.IntegrationType.REI,
                        HandlerFactory.HandlerType.FOLDER_SCREEN,
                        Object.class
                    );
                    
                    // Handler created successfully, register ingredient click handler
                    folderScreen.registerIngredientClickHandler((slot, button, shift, ctrl) -> 
                        handleIngredientClick(slot, button, shift, ctrl));
                    
                    DebugLogger.debugValue(
                        DebugLogger.Category.INTEGRATION,
                        "Connected folder screen to REI via factory", ""
                    );
                } catch (Exception factoryException) {
                    // Fallback to direct connection
                    DebugLogger.debugValue(
                        DebugLogger.Category.INTEGRATION,
                        "Factory creation failed, using direct connection: {}", 
                        factoryException.getMessage()
                    );
                    
                    folderScreen.registerIngredientClickHandler((slot, button, shift, ctrl) -> 
                        handleIngredientClick(slot, button, shift, ctrl));
                }
                
                EnoughFolders.LOGGER.debug("Connected folder screen to REI");
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
        BaseRecipeGuiHandler.saveLastFolderScreen(folderScreen);
    }
    
    /**
     * Clear the saved folder screen when no longer needed.
     */
    @Override
    public void clearLastFolderScreen() {
        BaseRecipeGuiHandler.clearLastFolderScreen();
    }
    
    /**
     * Get the last folder screen saved for recipe GUI navigation.
     * 
     * @return Optional containing the folder screen if available
     */
    @Override
    public Optional<FolderScreen> getLastFolderScreen() {
        return BaseRecipeGuiHandler.getLastFolderScreen();
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
            // Using centralized stack trace utility for REI transitions
            return StackTraceUtils.isREIRecipeTransition();
        } catch (Exception e) {
            EnoughFolders.LOGGER.debug("Error checking for REI recipe transition: {}", e.getMessage());
        }
        
        return false;
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
     * Get the display name of this integration.
     * 
     * @return The display name
     */
    @Override
    public String getDisplayName() {
        return getModName();
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
            ingredientManager.renderIngredient(graphics, ingredient, x, y, width, height);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error rendering REI ingredient: {}", 
                e.getMessage()
            );
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
    public boolean handleIngredientClick(IngredientSlot slot, int button, 
                                          boolean shift, boolean ctrl) {
        try {
            return recipeManager.handleIngredientClick(slot, button, shift, ctrl);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error handling REI ingredient click: {}", 
                e.getMessage()
            );
            return false;
        }
    }
}
