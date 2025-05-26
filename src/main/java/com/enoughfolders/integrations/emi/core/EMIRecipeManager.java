package com.enoughfolders.integrations.emi.core;

import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.integrations.api.FolderTarget;
import com.enoughfolders.integrations.api.FolderTargetStub;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages recipe viewing operations for EMI integration.
 */
public class EMIRecipeManager {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private EMIRecipeManager() {
        // Utility class should not be instantiated
    }
    
    private static boolean initialized = false;
    private static FolderScreen lastFolderScreen = null;
    
    /**
     * Initialize the EMI recipe manager.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Initializing EMI recipe manager", ""
            );
            
            initialized = true;
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI recipe manager initialized", ""
            );
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error initializing EMI recipe manager: {}", 
                e.getMessage()
            );
        }
    }

    /**
     * Save the last folder screen for recipe viewing integration.
     * @param folderScreen The folder screen to save
     */
    public static void saveLastFolderScreen(FolderScreen folderScreen) {
        lastFolderScreen = folderScreen;
        DebugLogger.debugValue(
            DebugLogger.Category.INTEGRATION,
            "Saved EMI folder screen reference", ""
        );
    }

    /**
     * Clear the saved folder screen reference.
     */
    public static void clearLastFolderScreen() {
        lastFolderScreen = null;
        DebugLogger.debugValue(
            DebugLogger.Category.INTEGRATION,
            "Cleared EMI folder screen reference", ""
        );
    }

    /**
     * Get the last saved folder screen.
     * @return An optional containing the last folder screen, or empty if none saved
     */
    public static Optional<FolderScreen> getLastFolderScreen() {
        return Optional.ofNullable(lastFolderScreen);
    }

    /**
     * Check if the given screen is an EMI recipe screen.
     * @param screen The screen to check
     * @return true if the screen is an EMI recipe screen, false otherwise
     */
    public static boolean isRecipeScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        
        try {
            String className = screen.getClass().getName();
            // Check for EMI recipe screen classes
            return className.contains("dev.emi.emi") && 
                   (className.contains("recipe") || className.contains("Recipe"));
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
     * Create folder targets for the given folder buttons.
     * @param folderButtons The folder buttons to create targets for
     * @return A list of folder targets
     */
    public static List<? extends FolderTarget> createFolderTargets(List<FolderButton> folderButtons) {
        List<FolderTarget> targets = new ArrayList<>();
        
        try {
            if (!initialized || folderButtons == null || folderButtons.isEmpty()) {
                return targets;
            }
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Creating EMI folder targets: {}", folderButtons.size()
            );
            
            // Use stub folder targets (drag functionality removed)
            for (FolderButton button : folderButtons) {
                FolderTarget target = new FolderTargetStub(button.getFolder());
                targets.add(target);
            }
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error creating EMI folder targets: {}", e.getMessage()
            );
        }
        
        return targets;
    }
    
    /**
     * Show recipes that produce the given ingredient.
     * @param ingredient The ingredient to show recipes for
     */
    public static void showRecipes(Object ingredient) {
        if (!initialized || ingredient == null) {
            return;
        }
        
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
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Displayed EMI recipes for ingredient", ""
            );
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error showing EMI recipes: {}", 
                e.getMessage()
            );
        }
    }
    
    /**
     * Show uses/usages of the given ingredient.
     * @param ingredient The ingredient to show uses for
     */
    public static void showUses(Object ingredient) {
        if (!initialized || ingredient == null) {
            return;
        }
        
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
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Displayed EMI uses for ingredient", ""
            );
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error showing EMI uses: {}", 
                e.getMessage()
            );
        }
    }
    
    /**
     * Convert an object to an EmiIngredient if possible.
     */
    private static Object convertToEmiIngredient(Object ingredient) {
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
            if (ingredient instanceof net.minecraft.world.item.ItemStack) {
                net.minecraft.world.item.ItemStack itemStack = (net.minecraft.world.item.ItemStack) ingredient;
                java.lang.reflect.Method ofMethod = emiStackClass.getMethod("of", net.minecraft.world.item.ItemStack.class);
                return ofMethod.invoke(null, itemStack);
            }
            
            return null;
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error converting to EMI ingredient: {}", 
                e.getMessage()
            );
            return null;
        }
    }
    
    /**
     * Get the current hovered ingredient from EMI.
     * @return The currently hovered ingredient, or null if none
     */
    public static Object getHoveredIngredient() {
        if (!initialized) {
            return null;
        }
        
        try {
            Class<?> emiApiClass = Class.forName("dev.emi.emi.api.EmiApi");
            
            java.lang.reflect.Method getHoveredStackMethod = emiApiClass.getMethod("getHoveredStack", boolean.class);
            Object stackInteraction = getHoveredStackMethod.invoke(null, true);
            
            if (stackInteraction != null) {
                // Get the stack from the interaction
                java.lang.reflect.Method getStackMethod = stackInteraction.getClass().getMethod("getStack");
                Object stack = getStackMethod.invoke(stackInteraction);
                
                // Check if it's not empty
                if (stack != null) {
                    java.lang.reflect.Method isEmptyMethod = stack.getClass().getMethod("isEmpty");
                    boolean isEmpty = (Boolean) isEmptyMethod.invoke(stack);
                    if (!isEmpty) {
                        return stack;
                    }
                }
            }
            
            return null;
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error getting hovered EMI ingredient: {}", 
                e.getMessage()
            );
            return null;
        }
    }
}
