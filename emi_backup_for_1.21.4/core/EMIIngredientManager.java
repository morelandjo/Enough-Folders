package com.enoughfolders.integrations.emi.core;

import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.base.AbstractIngredientManager;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Manages ingredient conversion and rendering for EMI integration.
 */
public class EMIIngredientManager extends AbstractIngredientManager {
    
    /**
     * Creates a new EMI ingredient manager.
     */
    public EMIIngredientManager() {
        super("EMI");
    }
    
    /**
     * Performs the actual conversion from StoredIngredient to ingredient object.
     *
     * @param storedIngredient The stored ingredient to convert
     * @return The original ingredient object, or null if conversion failed
     */
    @Override
    protected Object doGetIngredientFromStored(StoredIngredient storedIngredient) {
        try {
            // Avoid compile-time dependencies
            Class<?> emiStackClass = Class.forName("dev.emi.emi.api.stack.EmiStack");
            
            // Get the value from stored ingredient
            String value = storedIngredient.getValue();
            
            if (value == null || value.isEmpty()) {
                return null;
            }
            
            // Create an ItemStack from the stored value
            ItemStack itemStack = createItemStackFromValue(value);
            
            if (itemStack.isEmpty()) {
                return null;
            }
            
            java.lang.reflect.Method ofMethod = emiStackClass.getMethod("of", ItemStack.class);
            Object emiStack = ofMethod.invoke(null, itemStack);
            
            return emiStack;
            
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Performs the actual conversion from ingredient object to StoredIngredient.
     *
     * @param ingredient The ingredient object to convert
     * @return The StoredIngredient, or null if conversion failed
     */
    @Override
    protected StoredIngredient doStoreIngredient(Object ingredient) {
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
                return null;
            }
            
            // Check if the stack is empty
            java.lang.reflect.Method isEmptyMethod = emiStack.getClass().getMethod("isEmpty");
            boolean isEmpty = (Boolean) isEmptyMethod.invoke(emiStack);
            if (isEmpty) {
                return null;
            }
            
            // Get ItemStack representation if it's an item
            try {
                java.lang.reflect.Method getItemStackMethod = emiStack.getClass().getMethod("getItemStack");
                ItemStack itemStack = (ItemStack) getItemStackMethod.invoke(emiStack);
                
                if (itemStack == null || itemStack.isEmpty()) {
                    return null;
                }
                
                // Create StoredIngredient from ItemStack
                ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
                if (registryName != null) {
                    String type = "minecraft:item";
                    String value = registryName.toString();
                    return new StoredIngredient(type, value);
                }
            } catch (NoSuchMethodException e) {
                // This might be a fluid or other type, try to get item representation
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Performs the actual conversion from ingredient to ItemStack.
     *
     * @param ingredient The ingredient to convert
     * @return The ItemStack representation, or null if conversion failed
     */
    @Override
    protected ItemStack doGetItemStackForDisplay(Object ingredient) {
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
                return null;
            }
            
            // Check if the stack is empty
            java.lang.reflect.Method isEmptyMethod = emiStack.getClass().getMethod("isEmpty");
            boolean isEmpty = (Boolean) isEmptyMethod.invoke(emiStack);
            if (isEmpty) {
                return null;
            }
            
            // Try to get ItemStack representation
            try {
                java.lang.reflect.Method getItemStackMethod = emiStack.getClass().getMethod("getItemStack");
                ItemStack itemStack = (ItemStack) getItemStackMethod.invoke(emiStack);
                
                if (itemStack != null && !itemStack.isEmpty()) {
                    return itemStack;
                }
            } catch (NoSuchMethodException e) {
                // This might be a fluid or other type that doesn't have an ItemStack representation
                return null;
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
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
            // Log silently since this is a fallback method
        }
        
        // Return empty stack if couldn't parse
        return ItemStack.EMPTY;
    }
}
