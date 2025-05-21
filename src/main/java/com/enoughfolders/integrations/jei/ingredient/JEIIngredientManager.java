package com.enoughfolders.integrations.jei.ingredient;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.jei.core.JEIRuntimeManager;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.runtime.IJeiRuntime;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Manages ingredient conversion and rendering for JEI integration.
 */
public class JEIIngredientManager {
    
    /**
     * The JEI runtime manager
     */
    private final JEIRuntimeManager runtimeManager;
    
    /**
     * Creates a new JEI ingredient manager.
     *
     * @param runtimeManager The JEI runtime manager
     */
    public JEIIngredientManager(JEIRuntimeManager runtimeManager) {
        this.runtimeManager = runtimeManager;
    }
    
    /**
     * Converts a StoredIngredient back to its original JEI ingredient object.
     *
     * @param storedIngredient The stored ingredient to convert
     * @return Optional containing the original ingredient object, or empty if conversion failed
     */
    public Optional<?> getIngredientFromStored(StoredIngredient storedIngredient) {
        if (!runtimeManager.hasRuntime()) {
            return Optional.empty();
        }
        
        try {
            String typeName = storedIngredient.getType();
            String value = storedIngredient.getValue();
            
            Optional<IJeiRuntime> runtimeOpt = runtimeManager.getJeiRuntime();
            if (runtimeOpt.isEmpty()) {
                return Optional.empty();
            }
            
            IJeiRuntime runtime = runtimeOpt.get();
            
            for (IIngredientType<?> type : runtime.getIngredientManager().getRegisteredIngredientTypes()) {
                if (type.getIngredientClass().getName().equals(typeName)) {
                    @SuppressWarnings({"deprecation", "unchecked"})
                    Optional<? extends ITypedIngredient<?>> typedIngredient = 
                            runtime.getIngredientManager().getTypedIngredientByUid((IIngredientType) type, value);
                    
                    if (typedIngredient.isPresent()) {
                        return Optional.ofNullable(typedIngredient.get().getIngredient());
                    }
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to get ingredient from stored data", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Converts a JEI ingredient object into a StoredIngredient for persistence.
     *
     * @param ingredient The JEI ingredient object to convert
     * @return Optional containing the StoredIngredient, or empty if conversion failed
     */
    public Optional<StoredIngredient> storeIngredient(Object ingredient) {
        if (!runtimeManager.hasRuntime()) {
            return Optional.empty();
        }
        
        try {
            Optional<IJeiRuntime> runtimeOpt = runtimeManager.getJeiRuntime();
            if (runtimeOpt.isEmpty()) {
                return Optional.empty();
            }
            
            IJeiRuntime runtime = runtimeOpt.get();
            
            Optional<? extends ITypedIngredient<?>> optTypedIngredient = runtime.getIngredientManager()
                    .createTypedIngredient(ingredient);
            
            if (optTypedIngredient.isPresent()) {
                ITypedIngredient<?> typedIngredient = optTypedIngredient.get();
                IIngredientType<?> ingredientType = typedIngredient.getType();
                IIngredientHelper<Object> helper = getHelperForType(ingredientType);
                
                if (helper != null) {
                    String typeClass = ingredientType.getIngredientClass().getName();
                    Object uid = helper.getUid(ingredient, UidContext.Ingredient);
                    
                    return Optional.of(new StoredIngredient(typeClass, uid.toString()));
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to store ingredient", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Gets an ItemStack that can be used to visually represent the ingredient.
     *
     * @param ingredient The ingredient to get an ItemStack for
     * @return Optional containing the ItemStack, or empty if conversion failed
     */
    public Optional<ItemStack> getItemStackForDisplay(Object ingredient) {
        if (!runtimeManager.hasRuntime()) {
            return Optional.empty();
        }
        
        try {
            if (ingredient instanceof ItemStack itemStack) {
                return Optional.of(itemStack);
            }
            
            Optional<IJeiRuntime> runtimeOpt = runtimeManager.getJeiRuntime();
            if (runtimeOpt.isEmpty()) {
                return Optional.empty();
            }
            
            IJeiRuntime runtime = runtimeOpt.get();
            
            Optional<? extends ITypedIngredient<?>> optTypedIngredient = runtime.getIngredientManager()
                    .createTypedIngredient(ingredient);
            
            if (optTypedIngredient.isPresent()) {
                ITypedIngredient<?> typedIngredient = optTypedIngredient.get();
                IIngredientHelper<Object> helper = getHelperForType(typedIngredient.getType());
                if (helper != null) {
                    return Optional.ofNullable(helper.getCheatItemStack(ingredient));
                }
                
                if (typedIngredient.getType() instanceof IIngredientTypeWithSubtypes) {
                    if (typedIngredient.getIngredient() instanceof ItemStack) {
                        return Optional.of((ItemStack) typedIngredient.getIngredient());
                    }
                    
                    @SuppressWarnings("unchecked")
                    IIngredientTypeWithSubtypes<Item, ItemStack> itemType = 
                            (IIngredientTypeWithSubtypes<Item, ItemStack>) VanillaTypes.ITEM_STACK;
                    
                    if (ingredient instanceof Item item) {
                        return Optional.of(itemType.getDefaultIngredient(item));
                    }
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to get ItemStack for display", e);
        }
        
        return Optional.empty();
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
    public void renderIngredient(GuiGraphics graphics, StoredIngredient ingredient, int x, int y, int width, int height) {
        if (!runtimeManager.hasRuntime()) {
            return;
        }

        try {
            Optional<?> ingredientOpt = getIngredientFromStored(ingredient);
            if (ingredientOpt.isEmpty()) {
                return;
            }

            Object ingredientObj = ingredientOpt.get();
            
            Optional<IJeiRuntime> runtimeOpt = runtimeManager.getJeiRuntime();
            if (runtimeOpt.isEmpty()) {
                return;
            }
            
            IJeiRuntime runtime = runtimeOpt.get();
            
            for (IIngredientType<?> type : runtime.getIngredientManager().getRegisteredIngredientTypes()) {
                if (type.getIngredientClass().isInstance(ingredientObj)) {
                    @SuppressWarnings("unchecked")
                    var renderer = runtime.getIngredientManager().getIngredientRenderer((IIngredientType<Object>)type);
                    
                    if (renderer != null) {
                        renderer.render(graphics, ingredientObj, x, y);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to render ingredient", e);
        }
    }
    
    /**
     * Helper method to get the ingredient helper for a specific ingredient type.
     *
     * @param <T> The type of ingredient
     * @param type The ingredient type class
     * @return The ingredient helper, or null if not found
     */
    @SuppressWarnings("unchecked")
    private <T> IIngredientHelper<T> getHelperForType(IIngredientType<?> type) {
        try {
            Optional<IJeiRuntime> runtimeOpt = runtimeManager.getJeiRuntime();
            if (runtimeOpt.isEmpty()) {
                return null;
            }
            
            IJeiRuntime runtime = runtimeOpt.get();
            
            return (IIngredientHelper<T>) runtime.getIngredientManager().getIngredientHelper((IIngredientType<T>) type);
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to get ingredient helper for type: " + type, e);
            return null;
        }
    }
}
