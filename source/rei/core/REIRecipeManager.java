package com.enoughfolders.integrations.rei.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.integrations.base.AbstractRecipeManager;
import com.enoughfolders.integrations.rei.gui.handlers.REIRecipeGuiHandler;
import com.enoughfolders.client.gui.IngredientSlot;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.data.StoredIngredient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
            // Save current folder screen before showing recipes
            saveCurrentFolderScreen();
            
            // Use REI's modern ViewSearchBuilder API to show recipes
            Class<?> viewSearchBuilderClass = Class.forName("me.shedaniel.rei.api.client.view.ViewSearchBuilder");
            Class<?> entryStackClass = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");
            
            // Convert the ingredient to REI EntryStack if needed
            Object reiIngredient = convertToREIIngredient(ingredient);
            if (reiIngredient == null) {
                return;
            }
            
            // Create ViewSearchBuilder using builder() method
            java.lang.reflect.Method builderMethod = viewSearchBuilderClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            
            // Add recipes for the ingredient using addRecipesFor()
            java.lang.reflect.Method addRecipesForMethod = builder.getClass().getMethod("addRecipesFor", entryStackClass);
            Object builderWithRecipes = addRecipesForMethod.invoke(builder, reiIngredient);
            
            // Open the view using open() method
            java.lang.reflect.Method openMethod = builderWithRecipes.getClass().getMethod("open");
            openMethod.invoke(builderWithRecipes);
            
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
            // Save current folder screen before showing uses
            saveCurrentFolderScreen();
            
            // Use REI's modern ViewSearchBuilder API to show uses
            Class<?> viewSearchBuilderClass = Class.forName("me.shedaniel.rei.api.client.view.ViewSearchBuilder");
            Class<?> entryStackClass = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");
            
            // Convert the ingredient to REI EntryStack if needed
            Object reiIngredient = convertToREIIngredient(ingredient);
            if (reiIngredient == null) {
                return;
            }
            
            // Create ViewSearchBuilder using builder() method
            java.lang.reflect.Method builderMethod = viewSearchBuilderClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            
            // Add uses for the ingredient using addUsagesFor()
            java.lang.reflect.Method addUsagesForMethod = builder.getClass().getMethod("addUsagesFor", entryStackClass);
            Object builderWithUsages = addUsagesForMethod.invoke(builder, reiIngredient);
            
            // Open the view using open() method
            java.lang.reflect.Method openMethod = builderWithUsages.getClass().getMethod("open");
            openMethod.invoke(builderWithUsages);
            
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
            Class<?> entryStacksClass = Class.forName("me.shedaniel.rei.api.common.util.EntryStacks");
            
            // If it's already an EntryStack, return it
            if (entryStackClass.isInstance(ingredient)) {
                return ingredient;
            }
            
            // If it's an ItemStack, convert it using EntryStacks.of(ItemStack)
            if (ingredient instanceof ItemStack) {
                ItemStack itemStack = (ItemStack) ingredient;
                
                java.lang.reflect.Method ofMethod = entryStacksClass.getMethod("of", ItemStack.class);
                return ofMethod.invoke(null, itemStack);
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
            // Get the REI overlay and try to get the focused stack (hovered ingredient)
            Class<?> reiRuntimeClass = Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            java.lang.reflect.Method getRuntimeInstanceMethod = reiRuntimeClass.getMethod("getInstance");
            Object runtime = getRuntimeInstanceMethod.invoke(null);
            
            if (runtime != null) {
                // Get the overlay - this returns an Optional<ScreenOverlay>
                java.lang.reflect.Method getOverlayMethod = runtime.getClass().getMethod("getOverlay");
                Object overlayOptional = getOverlayMethod.invoke(runtime);
                
                if (overlayOptional != null) {
                    // Check if the Optional is present
                    java.lang.reflect.Method isPresentMethod = overlayOptional.getClass().getMethod("isPresent");
                    Boolean isPresent = (Boolean) isPresentMethod.invoke(overlayOptional);
                    
                    if (isPresent) {
                        // Get the actual overlay from the Optional
                        java.lang.reflect.Method getMethod = overlayOptional.getClass().getMethod("get");
                        Object overlay = getMethod.invoke(overlayOptional);
                        
                        EnoughFolders.LOGGER.debug("Got REI overlay from Optional: {}", overlay.getClass().getName());
                        
                        // Try to get the focused stack using getFocusedStack - this is the main method REI uses
                        try {
                            java.lang.reflect.Method getFocusedStackMethod = overlay.getClass().getMethod("getFocusedStack");
                            Object focusedStack = getFocusedStackMethod.invoke(overlay);
                            if (focusedStack != null) {
                                // Check if the stack is not empty
                                java.lang.reflect.Method isEmptyMethod = focusedStack.getClass().getMethod("isEmpty");
                                Boolean isEmpty = (Boolean) isEmptyMethod.invoke(focusedStack);
                                if (!isEmpty) {
                                    EnoughFolders.LOGGER.debug("Found focused stack from REI overlay: {}", focusedStack);
                                    return focusedStack;
                                }
                            }
                        } catch (NoSuchMethodException e) {
                            EnoughFolders.LOGGER.debug("getFocusedStack method not found on overlay: {}", e.getMessage());
                        }
                        
                        // Alternative: Try to get entry list and check for focused stack
                        try {
                            java.lang.reflect.Method getEntryListMethod = overlay.getClass().getMethod("getEntryList");
                            Object entryList = getEntryListMethod.invoke(overlay);
                            
                            if (entryList != null) {
                                java.lang.reflect.Method getFocusedStackMethod = entryList.getClass().getMethod("getFocusedStack");
                                Object focusedStack = getFocusedStackMethod.invoke(entryList);
                                if (focusedStack != null) {
                                    java.lang.reflect.Method isEmptyMethod = focusedStack.getClass().getMethod("isEmpty");
                                    Boolean isEmpty = (Boolean) isEmptyMethod.invoke(focusedStack);
                                    if (!isEmpty) {
                                        EnoughFolders.LOGGER.debug("Found focused stack from entry list: {}", focusedStack);
                                        return focusedStack;
                                    }
                                }
                            }
                        } catch (NoSuchMethodException e) {
                            EnoughFolders.LOGGER.debug("getEntryList method not found on overlay: {}", e.getMessage());
                        }
                        
                        // Try to get favorites list and check for focused stack
                        try {
                            java.lang.reflect.Method getFavoritesListMethod = overlay.getClass().getMethod("getFavoritesList");
                            Object favoritesList = getFavoritesListMethod.invoke(overlay);
                            
                            if (favoritesList != null) {
                                java.lang.reflect.Method getFocusedStackMethod = favoritesList.getClass().getMethod("getFocusedStack");
                                Object focusedStack = getFocusedStackMethod.invoke(favoritesList);
                                if (focusedStack != null) {
                                    java.lang.reflect.Method isEmptyMethod = focusedStack.getClass().getMethod("isEmpty");
                                    Boolean isEmpty = (Boolean) isEmptyMethod.invoke(focusedStack);
                                    if (!isEmpty) {
                                        EnoughFolders.LOGGER.debug("Found focused stack from favorites list: {}", focusedStack);
                                        return focusedStack;
                                    }
                                }
                            }
                        } catch (NoSuchMethodException e) {
                            EnoughFolders.LOGGER.debug("getFavoritesList method not found on overlay: {}", e.getMessage());
                        }
                    } else {
                        EnoughFolders.LOGGER.debug("REI overlay Optional is not present");
                    }
                }
            }
            
            EnoughFolders.LOGGER.debug("No hovered ingredient found in REI using getFocusedStack approach");
            return null;
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to get hovered ingredient from REI: {}", e.getMessage());
            EnoughFolders.LOGGER.debug("Full stack trace:", e);
            return null;
        }
    }
    
    /**
     * Saves the current folder screen so it can be displayed on recipe screens.
     */
    private void saveCurrentFolderScreen() {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            com.enoughfolders.client.event.ClientEventHandler.getFolderScreen(containerScreen)
                .ifPresent(folderScreen -> {
                    // Force a reinit of the folder screen before saving it
                    int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    folderScreen.init(screenWidth, screenHeight);
                    
                    // Use the handler's saveLastFolderScreen method instead of our own
                    REIRecipeGuiHandler.saveLastFolderScreen(folderScreen);
                    logDebug("Saved folder screen for recipe/usage view via REIRecipeGuiHandler");
                });
        }
    }
}
