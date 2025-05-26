package com.enoughfolders.integrations.jei.ingredient;

import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.base.AbstractIngredientManager;
import com.enoughfolders.integrations.jei.core.JEIRuntimeManager;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.runtime.IJeiRuntime;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Manages ingredient conversion and rendering for JEI integration.
 */
public class JEIIngredientManager extends AbstractIngredientManager {
    
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
        super("JEI");
        this.runtimeManager = runtimeManager;
    }
    
    /**
     * Performs the actual conversion from StoredIngredient to ingredient object.
     *
     * @param storedIngredient The stored ingredient to convert
     * @return The original ingredient object, or null if conversion failed
     */
    @Override
    protected Object doGetIngredientFromStored(StoredIngredient storedIngredient) {
        if (!runtimeManager.hasRuntime()) {
            return null;
        }
        
        String typeName = storedIngredient.getType();
        String value = storedIngredient.getValue();
        
        Optional<IJeiRuntime> runtimeOpt = runtimeManager.getJeiRuntime();
        if (runtimeOpt.isEmpty()) {
            return null;
        }
        
        IJeiRuntime runtime = runtimeOpt.get();
        
        // First try direct JEI format lookup
        for (IIngredientType<?> type : runtime.getIngredientManager().getRegisteredIngredientTypes()) {
            if (type.getIngredientClass().getName().equals(typeName)) {
                IIngredientHelper<Object> helper = getHelperForType(type);
                if (helper != null) {
                    try {
                        // Try to find the ingredient by recreating it from the UID
                        for (Object ingredient : runtime.getIngredientManager().getAllIngredients(type)) {
                            String ingredientUid = helper.getUid(ingredient, UidContext.Ingredient).toString();
                            if (value.equals(ingredientUid)) {
                                return ingredient;
                            }
                        }
                    } catch (Exception e) {
                        logDebug("Failed to match ingredient by UID for type {}: {}", typeName, e.getMessage());
                    }
                }
            }
        }
        
        // If direct lookup failed, try cross-integration compatibility
        Optional<?> crossIntegrationResult = tryConvertFromOtherIntegration(storedIngredient, runtime);
        return crossIntegrationResult.orElse(null);
    }
    
    /**
     * Performs the actual conversion from ingredient object to StoredIngredient.
     *
     * @param ingredient The ingredient object to convert
     * @return The StoredIngredient, or null if conversion failed
     */
    @Override
    protected StoredIngredient doStoreIngredient(Object ingredient) {
        if (!runtimeManager.hasRuntime()) {
            return null;
        }
        
        Optional<IJeiRuntime> runtimeOpt = runtimeManager.getJeiRuntime();
        if (runtimeOpt.isEmpty()) {
            return null;
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
                
                return new StoredIngredient(typeClass, uid.toString());
            }
        }
        
        return null;
    }
    
    /**
     * Performs the actual conversion from ingredient to ItemStack.
     *
     * @param ingredient The ingredient to convert
     * @return The ItemStack representation, or null if conversion failed
     */
    @Override
    protected ItemStack doGetItemStackForDisplay(Object ingredient) {
        if (!runtimeManager.hasRuntime()) {
            return null;
        }
        
        if (ingredient instanceof ItemStack itemStack) {
            return itemStack;
        }
        
        Optional<IJeiRuntime> runtimeOpt = runtimeManager.getJeiRuntime();
        if (runtimeOpt.isEmpty()) {
            return null;
        }
        
        IJeiRuntime runtime = runtimeOpt.get();
        
        Optional<? extends ITypedIngredient<?>> optTypedIngredient = runtime.getIngredientManager()
                .createTypedIngredient(ingredient);
        
        if (optTypedIngredient.isPresent()) {
            ITypedIngredient<?> typedIngredient = optTypedIngredient.get();
            IIngredientHelper<Object> helper = getHelperForType(typedIngredient.getType());
            if (helper != null) {
                ItemStack cheatStack = helper.getCheatItemStack(ingredient);
                if (cheatStack != null) {
                    return cheatStack;
                }
            }
            
            if (typedIngredient.getType() instanceof IIngredientTypeWithSubtypes) {
                if (typedIngredient.getIngredient() instanceof ItemStack) {
                    return (ItemStack) typedIngredient.getIngredient();
                }
                
                IIngredientTypeWithSubtypes<Item, ItemStack> itemType = VanillaTypes.ITEM_STACK;
                
                if (ingredient instanceof Item item) {
                    return itemType.getDefaultIngredient(item);
                }
            }
        }
        
        return null;
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
            logError("Failed to render ingredient", e);
        }
    }
    
    /**
     * Helper method to get the ingredient helper for a specific ingredient type.
     *
     * @param <T> The type of ingredient
     * @param type The ingredient type class
     * @return The ingredient helper, or null if not found
     */
    private <T> IIngredientHelper<T> getHelperForType(IIngredientType<?> type) {
        try {
            Optional<IJeiRuntime> runtimeOpt = runtimeManager.getJeiRuntime();
            if (runtimeOpt.isEmpty()) {
                return null;
            }
            
            IJeiRuntime runtime = runtimeOpt.get();
            
            @SuppressWarnings("unchecked")
            IIngredientType<T> typedType = (IIngredientType<T>) type;
            return runtime.getIngredientManager().getIngredientHelper(typedType);
        } catch (Exception e) {
            logError("Failed to get ingredient helper for type: {}", type, e);
            return null;
        }
    }
    
    /**
     * Attempts to convert ingredients stored by other integrations (EMI, REI) to JEI format.
     *
     * @param storedIngredient The stored ingredient from another integration
     * @param runtime The JEI runtime instance
     * @return Optional containing the converted ingredient, or empty if conversion failed
     */
    private Optional<?> tryConvertFromOtherIntegration(StoredIngredient storedIngredient, IJeiRuntime runtime) {
        try {
            String typeName = storedIngredient.getType();
            String value = storedIngredient.getValue();
            
            // Handle EMI format: type="minecraft:item", value="minecraft:stone"
            if ("minecraft:item".equals(typeName)) {
                return convertEMIItemToJEI(value, runtime);
            }
            
            // Handle REI format if needed (REI typically uses similar format to EMI)
            if ("rei:item".equals(typeName)) {
                return convertEMIItemToJEI(value, runtime);
            }
            
            
            logDebug("No cross-integration conversion available for type: {}", typeName);
            
        } catch (Exception e) {
            logError("Failed to convert ingredient from other integration", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Converts an EMI-format item (ResourceLocation string) to a JEI ItemStack.
     *
     * @param resourceLocationString The ResourceLocation string (e.g., "minecraft:stone")
     * @param runtime The JEI runtime instance
     * @return Optional containing the converted ItemStack, or empty if conversion failed
     */
    private Optional<?> convertEMIItemToJEI(String resourceLocationString, IJeiRuntime runtime) {
        try {
            // Parse the ResourceLocation string
            ResourceLocation resourceLocation = ResourceLocation.parse(resourceLocationString);
            
            // Get the item from the registry
            Item item = BuiltInRegistries.ITEM.get(resourceLocation);
            
            if (item == null || item == Items.AIR) {
                logDebug("Item not found in registry: {}", resourceLocationString);
                return Optional.empty();
            }
            
            // Create an ItemStack
            ItemStack itemStack = new ItemStack(item);
            
            if (itemStack.isEmpty()) {
                return Optional.empty();
            }
            
            // Verify that JEI can handle this ItemStack
            Optional<? extends ITypedIngredient<?>> typedIngredient = runtime.getIngredientManager()
                    .createTypedIngredient(itemStack);
            
            if (typedIngredient.isPresent()) {
                logDebug("Successfully converted EMI ingredient {} to JEI ItemStack", resourceLocationString);
                return Optional.of(itemStack);
            }
            
        } catch (Exception e) {
            logError("Failed to convert EMI item to JEI: {}", resourceLocationString, e);
        }
        
        return Optional.empty();
    }
}
