package com.enoughfolders.integrations.util;

import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Utility class for common ingredient operations across all integrations.
 */
public final class IngredientUtils {

    private IngredientUtils() {
        // Utility class should not be instantiated
    }

    /**
     * Detects the type of an ingredient object.
     * 
     * @param ingredient The ingredient object to check
     * @return A string describing the ingredient type
     */
    public static String detectIngredientType(Object ingredient) {
        if (ingredient == null) {
            return "null";
        }
        
        Class<?> ingredientClass = ingredient.getClass();
        String className = ingredientClass.getSimpleName();
        
        // Handle common ingredient types
        if (ingredient instanceof ItemStack) {
            ItemStack stack = (ItemStack) ingredient;
            return String.format("ItemStack[%s x%d]", 
                stack.getItem().toString(), stack.getCount());
        }
        
        // For other types, return the class name and try to get useful info
        try {
            if (ingredientClass.getName().contains("jei")) {
                return "JEI-" + className;
            } else if (ingredientClass.getName().contains("emi")) {
                return "EMI-" + className;
            } else if (ingredientClass.getName().contains("rei")) {
                return "REI-" + className;
            } else {
                return className;
            }
        } catch (Exception e) {
            return "Unknown-" + className;
        }
    }

    /**
     * Checks if two ingredient objects are equivalent.
     * 
     * @param ingredient1 First ingredient to compare
     * @param ingredient2 Second ingredient to compare
     * @return true if ingredients are equivalent, false otherwise
     */
    public static boolean areIngredientsEquivalent(Object ingredient1, Object ingredient2) {
        if (ingredient1 == ingredient2) {
            return true;
        }
        
        if (ingredient1 == null || ingredient2 == null) {
            return false;
        }
        
        // Check if both are the same type
        if (!ingredient1.getClass().equals(ingredient2.getClass())) {
            return false;
        }
        
        // Handle ItemStack comparison
        if (ingredient1 instanceof ItemStack && ingredient2 instanceof ItemStack) {
            ItemStack stack1 = (ItemStack) ingredient1;
            ItemStack stack2 = (ItemStack) ingredient2;
            return ItemStack.isSameItemSameComponents(stack1, stack2);
        }
        
        // For other types, use equals method
        try {
            return ingredient1.equals(ingredient2);
        } catch (Exception e) {
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Error comparing ingredients of type {}: {}", 
                ingredient1.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * Validates that an ingredient object is not null and not empty.
     * 
     * @param ingredient The ingredient to validate
     * @return true if ingredient is valid, false otherwise
     */
    public static boolean isValidIngredient(Object ingredient) {
        if (ingredient == null) {
            return false;
        }
        
        // Handle ItemStack validation
        if (ingredient instanceof ItemStack) {
            ItemStack stack = (ItemStack) ingredient;
            return !stack.isEmpty();
        }
        
        // For other types, just check it's not null
        return true;
    }

    /**
     * Extracts a display name from an ingredient object.
     * 
     * @param ingredient The ingredient to get the name from
     * @return A display name for the ingredient
     */
    public static String getIngredientDisplayName(Object ingredient) {
        if (ingredient == null) {
            return "null";
        }
        
        try {
            // Handle ItemStack
            if (ingredient instanceof ItemStack) {
                ItemStack stack = (ItemStack) ingredient;
                if (stack.isEmpty()) {
                    return "Empty ItemStack";
                }
                return stack.getHoverName().getString();
            }
            
            // Try to get a meaningful name from the object
            String className = ingredient.getClass().getSimpleName();
            String toString = ingredient.toString();
            
            // If toString seems meaningful (not just class@hash), use it
            if (!toString.matches(".*@[a-fA-F0-9]+$")) {
                return className + ":" + toString;
            } else {
                return className;
            }
            
        } catch (Exception e) {
            return ingredient.getClass().getSimpleName() + "[error]";
        }
    }

    /**
     * Safely converts an ingredient to a StoredIngredient using a conversion function.
     * 
     * @param ingredient The ingredient to convert
     * @param converter The conversion function to use
     * @param integrationName The name of the integration for logging
     * @return Optional containing the StoredIngredient if conversion successful
     */
    public static Optional<StoredIngredient> safeConvertToStored(Object ingredient, 
                                                               java.util.function.Function<Object, Optional<StoredIngredient>> converter,
                                                               String integrationName) {
        if (!isValidIngredient(ingredient)) {
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Invalid ingredient provided to {} converter", integrationName);
            return Optional.empty();
        }
        
        try {
            String ingredientType = detectIngredientType(ingredient);
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Converting {} ingredient to StoredIngredient: {}", integrationName, ingredientType);
            
            Optional<StoredIngredient> result = converter.apply(ingredient);
            
            if (result.isPresent()) {
                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                    "Successfully converted {} ingredient: {} -> {}", 
                    integrationName, ingredientType, result.get().getValue());
            } else {
                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                    "Failed to convert {} ingredient: {}", integrationName, ingredientType);
            }
            
            return result;
            
        } catch (Exception e) {
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Error converting {} ingredient: {}", integrationName, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Compares two StoredIngredient objects for equality.
     * 
     * @param stored1 First StoredIngredient
     * @param stored2 Second StoredIngredient
     * @return true if they represent the same ingredient, false otherwise
     */
    public static boolean areStoredIngredientsEqual(StoredIngredient stored1, StoredIngredient stored2) {
        if (stored1 == stored2) {
            return true;
        }
        
        if (stored1 == null || stored2 == null) {
            return false;
        }
        
        return stored1.getType().equals(stored2.getType()) && 
               stored1.getValue().equals(stored2.getValue());
    }

    /**
     * Creates a hash code for consistent ingredient comparison.
     * 
     * @param ingredient The ingredient to hash
     * @return A hash code for the ingredient
     */
    public static int getIngredientHashCode(Object ingredient) {
        if (ingredient == null) {
            return 0;
        }
        
        try {
            // Handle ItemStack hashing
            if (ingredient instanceof ItemStack) {
                ItemStack stack = (ItemStack) ingredient;
                if (stack.isEmpty()) {
                    return 0;
                }
                // Hash based on item and components, not count
                return stack.getItem().hashCode() * 31 + stack.getComponents().hashCode();
            }
            
            // For other types, use their hashCode
            return ingredient.hashCode();
            
        } catch (Exception e) {
            // Fallback to class hash if ingredient's hashCode throws
            return ingredient.getClass().hashCode();
        }
    }

    /**
     * Logs detailed information about an ingredient for debugging.
     * 
     * @param ingredient The ingredient to log
     * @param context Additional context for the log message
     */
    public static void logIngredientDetails(Object ingredient, String context) {
        if (!DebugLogger.isEnabled(DebugLogger.Category.INTEGRATION)) {
            return; // Skip expensive operations if debug is disabled
        }
        
        String type = detectIngredientType(ingredient);
        String displayName = getIngredientDisplayName(ingredient);
        int hashCode = getIngredientHashCode(ingredient);
        boolean valid = isValidIngredient(ingredient);
        
        DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
            "Ingredient [{}]: type={}, name={}, hash={}, valid={}", 
            context, type, displayName, hashCode, valid);
    }
}
