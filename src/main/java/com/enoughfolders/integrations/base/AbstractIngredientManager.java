package com.enoughfolders.integrations.base;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Abstract base class for all ingredient managers.
 */
public abstract class AbstractIngredientManager {
    
    /**
     * The integration name for this ingredient manager
     */
    protected final String integrationName;
    
    /**
     * Creates a new abstract ingredient manager.
     *
     * @param integrationName The name of the integration this manager belongs to
     */
    protected AbstractIngredientManager(String integrationName) {
        this.integrationName = integrationName;
    }
    
    /**
     * Converts a StoredIngredient back to its original ingredient object.
     *
     * @param storedIngredient The stored ingredient to convert
     * @return Optional containing the original ingredient object, or empty if conversion failed
     */
    public Optional<?> getIngredientFromStored(StoredIngredient storedIngredient) {
        if (storedIngredient == null) {
            logDebug("StoredIngredient is null, cannot convert");
            return Optional.empty();
        }
        
        return safeExecute(
            () -> doGetIngredientFromStored(storedIngredient),
            "ingredient conversion from stored"
        );
    }
    
    /**
     * Performs the actual conversion from StoredIngredient to ingredient object.
     *
     * @param storedIngredient The stored ingredient to convert
     * @return The original ingredient object, or null if conversion failed
     */
    protected abstract Object doGetIngredientFromStored(StoredIngredient storedIngredient);
    
    /**
     * Converts an ingredient object to a StoredIngredient.
     *
     * @param ingredient The ingredient object to convert
     * @return Optional containing the StoredIngredient, or empty if conversion failed
     */
    public Optional<StoredIngredient> storeIngredient(Object ingredient) {
        if (ingredient == null) {
            logDebug("Ingredient is null, cannot store");
            return Optional.empty();
        }
        
        return safeExecute(
            () -> doStoreIngredient(ingredient),
            "ingredient storage"
        );
    }
    
    /**
     * Performs the actual conversion from ingredient object to StoredIngredient.
     *
     * @param ingredient The ingredient object to convert
     * @return The StoredIngredient, or null if conversion failed
     */
    protected abstract StoredIngredient doStoreIngredient(Object ingredient);
    
    /**
     * Gets an ItemStack representation of an ingredient for rendering.
     *
     * @param ingredient The ingredient to convert to an ItemStack
     * @return Optional containing the ItemStack representation, or empty if conversion failed
     */
    public Optional<ItemStack> getItemStackForDisplay(Object ingredient) {
        if (ingredient == null) {
            logDebug("Ingredient is null, cannot get ItemStack");
            return Optional.empty();
        }
        
        Optional<ItemStack> result = safeExecute(
            () -> doGetItemStackForDisplay(ingredient),
            "ItemStack conversion for display"
        );
        
        // Provide fallback if specific implementation returns null/empty
        if (result.isEmpty()) {
            result = getFallbackItemStack(ingredient);
        }
        
        return result;
    }
    
    /**
     * Performs the actual conversion from ingredient to ItemStack.
     *
     * @param ingredient The ingredient to convert
     * @return The ItemStack representation, or null if conversion failed
     */
    protected abstract ItemStack doGetItemStackForDisplay(Object ingredient);
    
    /**
     * Renders an ingredient at the specified position and size.
     *
     * @param graphics The GUI graphics context
     * @param ingredient The ingredient to render
     * @param x The X position
     * @param y The Y position
     * @param size The size to render at
     */
    public void renderIngredient(GuiGraphics graphics, Object ingredient, int x, int y, int size) {
        if (ingredient == null || graphics == null) {
            logDebug("Ingredient or graphics is null, cannot render");
            return;
        }
        
        safeExecute(
            () -> {
                doRenderIngredient(graphics, ingredient, x, y, size);
                return null;
            },
            "ingredient rendering"
        );
    }
    
    /**
     * Performs the actual ingredient rendering.
     *
     * @param graphics The GUI graphics context
     * @param ingredient The ingredient to render
     * @param x The X position
     * @param y The Y position
     * @param size The size to render at
     */
    protected void doRenderIngredient(GuiGraphics graphics, Object ingredient, int x, int y, int size) {
        // Default implementation: try to get ItemStack and render it
        Optional<ItemStack> itemStack = getItemStackForDisplay(ingredient);
        if (itemStack.isPresent() && !itemStack.get().isEmpty()) {
            graphics.renderItem(itemStack.get(), x, y);
        } else {
            logDebug("Cannot render ingredient: no ItemStack representation available");
        }
    }
    
    /**
     * Provides a fallback ItemStack when the specific implementation fails.
     *
     * @param ingredient The ingredient to get a fallback for
     * @return Optional containing a fallback ItemStack, or empty if no fallback available
     */
    protected Optional<ItemStack> getFallbackItemStack(Object ingredient) {
        // Default fallback: return barrier item to indicate missing ingredient
        return Optional.of(new ItemStack(Items.BARRIER));
    }
    
    /**
     * Checks if an ingredient is valid for this manager.
     *
     * @param ingredient The ingredient to validate
     * @return true if valid, false otherwise
     */
    protected boolean isValidIngredient(Object ingredient) {
        return ingredient != null;
    }
    
    /**
     * Creates an ItemStack from a resource location and optional count.
     *
     * @param resourceLocation The resource location of the item
     * @param count The count (optional, defaults to 1)
     * @return Optional containing the created ItemStack, or empty if creation failed
     */
    protected Optional<ItemStack> createItemStack(ResourceLocation resourceLocation, int count) {
        try {
            Item item = BuiltInRegistries.ITEM.get(resourceLocation);
            if (item != null && item != Items.AIR) {
                return Optional.of(new ItemStack(item, Math.max(1, count)));
            }
        } catch (Exception e) {
            logDebug("Failed to create ItemStack for {}: {}", resourceLocation, e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Creates an ItemStack from a resource location with count 1.
     *
     * @param resourceLocation The resource location of the item
     * @return Optional containing the created ItemStack, or empty if creation failed
     */
    protected Optional<ItemStack> createItemStack(ResourceLocation resourceLocation) {
        return createItemStack(resourceLocation, 1);
    }
    
    /**
     * Safely executes an operation that might throw an exception.
     *
     * @param operation The operation to execute
     * @param operationName The name of the operation for logging
     * @param <T> The return type
     * @return Optional containing the result, or empty if an exception occurred
     */
    protected <T> Optional<T> safeExecute(SafeOperation<T> operation, String operationName) {
        try {
            T result = operation.execute();
            return Optional.ofNullable(result);
        } catch (Exception e) {
            logError("Error during {}: {}", operationName, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Logs an info message with ingredient manager context.
     *
     * @param message The message format
     * @param args The message arguments
     */
    protected void logInfo(String message, Object... args) {
        String formattedMessage = String.format("[%s IngredientManager] %s", integrationName, message);
        EnoughFolders.LOGGER.info(formattedMessage, args);
    }
    
    /**
     * Logs a debug message with ingredient manager context.
     *
     * @param message The message format
     * @param args The message arguments
     */
    protected void logDebug(String message, Object... args) {
        String formattedMessage = String.format("[%s IngredientManager] %s", integrationName, message);
        EnoughFolders.LOGGER.debug(formattedMessage, args);
        
        // Also log to DebugLogger if available
        DebugLogger.debugValue(
            DebugLogger.Category.INTEGRATION,
            formattedMessage, 
            args.length > 0 ? args[0] : ""
        );
    }
    
    /**
     * Logs an error message with ingredient manager context.
     *
     * @param message The message format
     * @param args The message arguments (last argument should be exception if present)
     */
    protected void logError(String message, Object... args) {
        String formattedMessage = String.format("[%s IngredientManager] %s", integrationName, message);
        
        // Check if last argument is an exception
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            Object[] messageArgs = new Object[args.length - 1];
            System.arraycopy(args, 0, messageArgs, 0, args.length - 1);
            Throwable exception = (Throwable) args[args.length - 1];
            EnoughFolders.LOGGER.error(formattedMessage, messageArgs, exception);
        } else {
            EnoughFolders.LOGGER.error(formattedMessage, args);
        }
    }
    
    /**
     * Functional interface for safe operations.
     *
     * @param <T> The return type of the operation
     */
    @FunctionalInterface
    protected interface SafeOperation<T> {
        /**
         * Executes the operation and returns the result.
         *
         * @return The result of the operation
         * @throws Exception If the operation fails
         */
        T execute() throws Exception;
    }
}
