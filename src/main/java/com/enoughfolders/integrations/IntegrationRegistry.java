package com.enoughfolders.integrations;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.StoredIngredient;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registry for all mod integrations.
 */
/**
 * Registry for mod integrations that provides access to integrated mod functionality.
 */
public class IntegrationRegistry {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private IntegrationRegistry() {
        // Utility class should not be instantiated
    }
    private static final List<ModIntegration> integrations = new ArrayList<>();
    private static boolean initialized = false;
    
    // Deprecated initialize() method removed - integrations are now managed through DI system
    
    /**
     * Register a mod integration.
     * 
     * @param integration The integration implementation to register
     */
    public static void register(ModIntegration integration) {
        if (integration.isAvailable()) {
            integrations.add(integration);
            EnoughFolders.LOGGER.info("Registered integration: " + integration.getClass().getSimpleName());
            
            // Initialize drag and drop support
            integration.registerDragAndDrop();
        }
    }
    
    /**
     * Get an integration by class.
     * 
     * @param <T> The type of integration to retrieve
     * @param clazz The class of the integration to retrieve
     * @return Optional containing the integration if found, empty otherwise
     */
    @SuppressWarnings("unchecked")
    public static <T extends ModIntegration> Optional<T> getIntegration(Class<T> clazz) {
        for (ModIntegration integration : integrations) {
            if (clazz.isInstance(integration)) {
                return Optional.of((T) integration);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Get an integration by class name.
     * 
     * @param className The fully qualified class name of the integration to retrieve
     * @return Optional containing the integration if found, empty otherwise
     */
    public static Optional<ModIntegration> getIntegrationByClassName(String className) {
        for (ModIntegration integration : integrations) {
            if (integration.getClass().getName().equals(className)) {
                return Optional.of(integration);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Find an integration that can handle the given ingredient.
     * 
     * @param ingredient The stored ingredient to find a handler for
     * @return The integration that can handle the ingredient, or null if none found
     */
    public static ModIntegration findIntegrationFor(StoredIngredient ingredient) {
        for (ModIntegration integration : integrations) {
            if (integration.canHandleIngredient(ingredient)) {
                return integration;
            }
        }
        return null;
    }
    
    /**
     * Render an ingredient using the appropriate integration.
     * 
     * @param graphics The GUI graphics context to render with
     * @param ingredient The ingredient to render
     * @param x The x position to render at
     * @param y The y position to render at
     * @param width The width of the rendering area
     * @param height The height of the rendering area
     */
    public static void renderIngredient(GuiGraphics graphics, StoredIngredient ingredient, int x, int y, int width, int height) {
        ModIntegration integration = findIntegrationFor(ingredient);
        if (integration != null) {
            integration.renderIngredient(graphics, ingredient, x, y, width, height);
        }
    }
    
    /**
     * Convert an object to a StoredIngredient using the appropriate integration.
     * 
     * @param ingredientObj The object to convert to a stored ingredient
     * @return The created StoredIngredient, or null if no integration could convert the object
     */
    public static StoredIngredient createStoredIngredient(Object ingredientObj) {
        // Try each integration until one works
        for (ModIntegration integration : integrations) {
            StoredIngredient ingredient = integration.createStoredIngredient(ingredientObj);
            if (ingredient != null) {
                return ingredient;
            }
        }
        return null;
    }
    
    /**
     * Checks if a specific integration is available by ID.
     * 
     * @param integrationId The ID of the integration to check ("jei", "rei", "ftb", etc.)
     * @return true if the integration is available, false otherwise
     */
    public static boolean isIntegrationAvailable(String integrationId) {
        // Make sure the DI bridge is initialized
        if (!initialized) {
            com.enoughfolders.di.IntegrationRegistryBridge.initializeBridge();
            initialized = true;
        }
        
        // Check each integration
        for (ModIntegration integration : integrations) {
            if (getIntegrationIdFromClass(integration.getClass()).equalsIgnoreCase(integrationId) && 
                integration.isAvailable()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the integration ID from its class.
     * 
     * @param clazz The class of the integration
     * @return The ID of the integration (lowercase)
     */
    private static String getIntegrationIdFromClass(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        if (simpleName.endsWith("Integration")) {
            // Remove "Integration" suffix and convert to lowercase
            simpleName = simpleName.substring(0, simpleName.length() - 11);
        }
        return simpleName.toLowerCase();
    }
    
    /**
     * Gets all registered integrations that implement the IngredientDragProvider interface.
     *
     * @return List of all drag provider integrations
     */
    public static List<com.enoughfolders.integrations.api.IngredientDragProvider> getAllDragProviders() {
        List<com.enoughfolders.integrations.api.IngredientDragProvider> providers = new ArrayList<>();
        
        for (ModIntegration integration : integrations) {
            if (integration instanceof com.enoughfolders.integrations.api.IngredientDragProvider) {
                providers.add((com.enoughfolders.integrations.api.IngredientDragProvider) integration);
            }
        }
        
        return providers;
    }
}
