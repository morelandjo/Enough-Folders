package com.enoughfolders.integrations.jei.core;


import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.di.DependencyProvider;
import com.google.gson.Gson;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Helper utility for working with JEI ingredients.
 */
public class IngredientHelper {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private IngredientHelper() {
        // Utility class should not be instantiated
    }
    private static final Gson GSON = new Gson();

    /**
     * Converts a JEI ITypedIngredient to StoredIngredient.
     * 
     * @param <T> The type of ingredient
     * @param ingredientObject The ingredient object to convert
     * @return A StoredIngredient representation of the ingredient, or null if conversion failed
     */
    public static <T> StoredIngredient createStoredIngredient(Object ingredientObject) {
        // Get the JEI integration instance before trying to access runtime
        Optional<JEIIntegration> jeiIntegration = DependencyProvider.get(JEIIntegration.class);
        if (jeiIntegration.isEmpty()) {
            return null;
        }
        
        Optional<IJeiRuntime> jeiRuntimeOpt = jeiIntegration.get().getJeiRuntime();
        if (jeiRuntimeOpt.isEmpty()) {
            return null;
        }
        IJeiRuntime jeiRuntime = jeiRuntimeOpt.get();

        try {
            // Try to find the ingredient type
            Optional<?> optional = jeiRuntime.getIngredientManager().getIngredientTypeChecked(ingredientObject);
            if (optional.isEmpty()) {
                return null;
            }

            IIngredientType<?> ingredientType = (IIngredientType<?>) optional.get();
            String typeName = ingredientType.getIngredientClass().getName();

            // Handle ItemStack specifically
            if (ingredientObject instanceof ItemStack itemStack) {
                Item item = itemStack.getItem();
                ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
                String value = registryName.toString();

                // Get component data if present
                DataComponentMap components = itemStack.getComponents();
                if (!components.isEmpty()) {
                    // Serialize block entity data
                    var blockEntityData = components.get(DataComponents.BLOCK_ENTITY_DATA);
                    if (blockEntityData != null) {
                        value += "#BlockEntityData:" + blockEntityData.toString();
                    }
                }

                return new StoredIngredient("item", value);
            }

            // For other ingredient types, use JSON serialization
            String serialized = GSON.toJson(ingredientObject);
            return new StoredIngredient(typeName, serialized);
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error creating stored ingredient", e);
            return null;
        }
    }

    /**
     * Converts StoredIngredient back to a JEI ITypedIngredient.
     * 
     * @param <T> The ingredient type
     * @param storedIngredient The stored ingredient to convert
     * @return The JEI typed ingredient, or null if conversion failed
     */
    @SuppressWarnings("unchecked")
    public static <T> ITypedIngredient<T> getTypedIngredientFromStored(StoredIngredient storedIngredient) {
        // Get the JEI integration instance before trying to access runtime
        Optional<JEIIntegration> jeiIntegration = DependencyProvider.get(JEIIntegration.class);
        if (jeiIntegration.isEmpty()) {
            return null;
        }
        
        Optional<IJeiRuntime> jeiRuntimeOpt = jeiIntegration.get().getJeiRuntime();
        if (jeiRuntimeOpt.isEmpty()) {
            return null;
        }
        IJeiRuntime jeiRuntime = jeiRuntimeOpt.get();

        try {
            String type = storedIngredient.getType();
            String value = storedIngredient.getValue();

            // Handle ItemStack specifically
            if ("item".equals(type)) {
                String[] parts = value.split("#", 2);
                ResourceLocation itemId = ResourceLocation.parse(parts[0]);
                Item item = BuiltInRegistries.ITEM.get(itemId);
                
                ItemStack itemStack = new ItemStack(item);
                
                // Handle component data if it was stored
                if (parts.length > 1 && parts[1].startsWith("BlockEntityData:")) {
                    EnoughFolders.LOGGER.debug("Found component data in stored ingredient: {}", parts[1]);
                }
                
                return (ITypedIngredient<T>) jeiRuntime.getIngredientManager().createTypedIngredient(itemStack).orElse(null);
            }

            return null;
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error getting typed ingredient", e);
            return null;
        }
    }
}
