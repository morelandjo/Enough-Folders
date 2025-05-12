package com.enoughfolders.integrations;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.jei.core.JEIIntegration;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registry for all mod integrations.
 */
public class IntegrationRegistry {
    private static final List<ModIntegration> integrations = new ArrayList<>();
    private static boolean initialized = false;
    
    /**
     * Initialize all integrations.
     */
    public static synchronized void initialize() {
        if (initialized) {
            com.enoughfolders.util.DebugLogger.debug(
                com.enoughfolders.util.DebugLogger.Category.INTEGRATION,
                "IntegrationRegistry already initialized, skipping"
            );
            return;
        }
        
        com.enoughfolders.util.DebugLogger.debug(
            com.enoughfolders.util.DebugLogger.Category.INTEGRATION,
            "Initializing IntegrationRegistry"
        );
        
        // Register all integrations here
        integrations.add(new JEIIntegration());
        
        // Initialize all integrations
        integrations.forEach(integration -> integration.initialize());
        initialized = true;
        
        com.enoughfolders.util.DebugLogger.debugValue(
            com.enoughfolders.util.DebugLogger.Category.INTEGRATION,
            "IntegrationRegistry initialization complete, integrations count: {}", 
            integrations.size()
        );
    }
    
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
}
