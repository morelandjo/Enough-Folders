package com.enoughfolders.integrations.emi.core;

import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Manages ingredient conversions between EMI and Enough Folders.
 */
public class EMIIngredientManager {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private EMIIngredientManager() {
        // Utility class should not be instantiated
    }
    
    private static boolean initialized = false;
    
    /**
     * Initialize the EMI ingredient manager.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            DebugLogger.debug(
                DebugLogger.Category.INTEGRATION,
                "Initializing EMI ingredient manager"
            );
            
            initialized = true;
            
            DebugLogger.debug(
                DebugLogger.Category.INTEGRATION,
                "EMI ingredient manager initialized"
            );
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error initializing EMI ingredient manager: {}", e.getMessage()
            );
        }
    }
    
    /**
     * Convert a StoredIngredient to an EMI ingredient object.
     * @param storedIngredient The stored ingredient to convert
     * @return An optional EMI ingredient object, empty if conversion fails
     */
    public static Optional<?> getIngredientFromStored(StoredIngredient storedIngredient) {
        if (!initialized || storedIngredient == null) {
            return Optional.empty();
        }
        
        try {
            //avoid compile-time dependencies
            Class<?> emiStackClass = Class.forName("dev.emi.emi.api.stack.EmiStack");
            
            // Get the value from stored ingredient
            String value = storedIngredient.getValue();
            
            if (value == null || value.isEmpty()) {
                return Optional.empty();
            }
            
            // Create an ItemStack from the stored value
            ItemStack itemStack = createItemStackFromValue(value);
            
            if (itemStack.isEmpty()) {
                DebugLogger.debugValue(
                    DebugLogger.Category.INTEGRATION,
                    "Could not create ItemStack from stored value: {}", value
                );
                return Optional.empty();
            }
            
            java.lang.reflect.Method ofMethod = emiStackClass.getMethod("of", ItemStack.class);
            Object emiStack = ofMethod.invoke(null, itemStack);
            
            return Optional.of(emiStack);
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error converting StoredIngredient to EMI ingredient: {}", e.getMessage()
            );
            return Optional.empty();
        }
    }
    
    /**
     * Convert an EMI ingredient object to a StoredIngredient.
     * @param ingredient The EMI ingredient to convert
     * @return An optional StoredIngredient, empty if conversion fails
     */
    public static Optional<StoredIngredient> storeIngredient(Object ingredient) {
        if (!initialized || ingredient == null) {
            return Optional.empty();
        }
        
        try {
            // Check if it's an EmiStack or EmiIngredient
            Class<?> emiStackClass = Class.forName("dev.emi.emi.api.stack.EmiStack");
            Class<?> emiIngredientClass = Class.forName("dev.emi.emi.api.stack.EmiIngredient");
            
            Object emiStack = null;
            
            if (emiStackClass.isInstance(ingredient)) {
                emiStack = ingredient;
            } else if (emiIngredientClass.isInstance(ingredient)) {
                // Get the first EmiStack from the ingredient
                java.lang.reflect.Method getEmiStacksMethod = ingredient.getClass().getMethod("getEmiStacks");
                @SuppressWarnings("unchecked")
                java.util.List<Object> emiStacks = (java.util.List<Object>) getEmiStacksMethod.invoke(ingredient);
                if (!emiStacks.isEmpty()) {
                    emiStack = emiStacks.get(0);
                }
            }
            
            if (emiStack == null) {
                return Optional.empty();
            }
            
            // Check if the stack is empty
            java.lang.reflect.Method isEmptyMethod = emiStack.getClass().getMethod("isEmpty");
            boolean isEmpty = (Boolean) isEmptyMethod.invoke(emiStack);
            if (isEmpty) {
                return Optional.empty();
            }
            
            // Get ItemStack representation if it's an item
            try {
                java.lang.reflect.Method getItemStackMethod = emiStack.getClass().getMethod("getItemStack");
                ItemStack itemStack = (ItemStack) getItemStackMethod.invoke(emiStack);
                
                if (itemStack == null || itemStack.isEmpty()) {
                    return Optional.empty();
                }
                
                
                // Create StoredIngredient from ItemStack
                ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
                if (registryName != null) {
                    String type = "minecraft:item";
                    String value = registryName.toString();
                    StoredIngredient stored = new StoredIngredient(type, value);
                    return Optional.of(stored);
                }
            } catch (NoSuchMethodException e) {
                // This might be a fluid or other type, try to get item representation
                DebugLogger.debugValue(
                    DebugLogger.Category.INTEGRATION,
                    "EMI ingredient is not an item stack: {}", e.getMessage()
                );
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error converting EMI ingredient to StoredIngredient: {}", e.getMessage()
            );
            return Optional.empty();
        }
    }
    
    /**
     * Get an ItemStack representation of an EMI ingredient for display purposes.
     * @param ingredient The EMI ingredient to get ItemStack for
     * @return An optional ItemStack for display, empty if not available
     */
    public static Optional<ItemStack> getItemStackForDisplay(Object ingredient) {
        if (!initialized || ingredient == null) {
            return Optional.empty();
        }
        
        try {
            // Check if it's an EmiStack or EmiIngredient
            Class<?> emiStackClass = Class.forName("dev.emi.emi.api.stack.EmiStack");
            Class<?> emiIngredientClass = Class.forName("dev.emi.emi.api.stack.EmiIngredient");
            
            Object emiStack = null;
            
            if (emiStackClass.isInstance(ingredient)) {
                emiStack = ingredient;
            } else if (emiIngredientClass.isInstance(ingredient)) {
                // Get the first EmiStack from the ingredient
                java.lang.reflect.Method getEmiStacksMethod = ingredient.getClass().getMethod("getEmiStacks");
                @SuppressWarnings("unchecked")
                java.util.List<Object> emiStacks = (java.util.List<Object>) getEmiStacksMethod.invoke(ingredient);
                if (!emiStacks.isEmpty()) {
                    emiStack = emiStacks.get(0);
                }
            }
            
            if (emiStack == null) {
                return Optional.empty();
            }
            
            // Check if the stack is empty
            java.lang.reflect.Method isEmptyMethod = emiStack.getClass().getMethod("isEmpty");
            boolean isEmpty = (Boolean) isEmptyMethod.invoke(emiStack);
            if (isEmpty) {
                return Optional.empty();
            }
            
            // Try to get ItemStack representation
            try {
                java.lang.reflect.Method getItemStackMethod = emiStack.getClass().getMethod("getItemStack");
                ItemStack itemStack = (ItemStack) getItemStackMethod.invoke(emiStack);
                
                if (itemStack != null && !itemStack.isEmpty()) {
                    return Optional.of(itemStack);
                }
            } catch (NoSuchMethodException e) {
                // This might be a fluid or other type that doesn't have an ItemStack representation
                DebugLogger.debugValue(
                    DebugLogger.Category.INTEGRATION,
                    "EMI ingredient does not have ItemStack representation: {}", e.getMessage()
                );
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error getting ItemStack for EMI ingredient: {}", e.getMessage()
            );
            return Optional.empty();
        }
    }
    
    /**
     * Check if an object is a valid EMI ingredient.
     * @param obj The object to check
     * @return true if the object is a valid EMI ingredient, false otherwise
     */
    public static boolean isEMIIngredient(Object obj) {
        if (obj == null) {
            return false;
        }
        
        try {
            Class<?> emiStackClass = Class.forName("dev.emi.emi.api.stack.EmiStack");
            Class<?> emiIngredientClass = Class.forName("dev.emi.emi.api.stack.EmiIngredient");
            
            return emiStackClass.isInstance(obj) || emiIngredientClass.isInstance(obj);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Create an ItemStack from a stored value string.
     */
    private static ItemStack createItemStackFromValue(String value) {
        try {
            // Basic format expected: "minecraft:stone" or something similar
            String[] parts = value.split(":", 2);
            ResourceLocation resourceLocation;
            
            if (parts.length == 2) {
                resourceLocation = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
            } else {
                resourceLocation = ResourceLocation.fromNamespaceAndPath("minecraft", value);
            }
            
            Item item = BuiltInRegistries.ITEM.get(resourceLocation);
            
            if (item != null && item != Items.AIR) {
                // Create a standard ItemStack with amount 1
                return new ItemStack(item);
            }
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION, 
                "Failed to create ItemStack from value: {}", e.getMessage()
            );
        }
        
        // Return empty stack if couldn't parse
        return ItemStack.EMPTY;
    }
}
