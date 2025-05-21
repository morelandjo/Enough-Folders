package com.enoughfolders.integrations;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.ftb.core.FTBLibraryIntegration;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import com.enoughfolders.integrations.rei.core.REIIntegration;

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
        
        // Check for system properties that affect which integrations to load
        boolean jeiOnly = Boolean.parseBoolean(System.getProperty("enoughfolders.jei_only", "false"));
        boolean reiOnly = Boolean.parseBoolean(System.getProperty("enoughfolders.rei_only", "false"));
        
        EnoughFolders.LOGGER.info("EnoughFolders: System property 'enoughfolders.jei_only' = {}", jeiOnly);
        EnoughFolders.LOGGER.info("EnoughFolders: System property 'enoughfolders.rei_only' = {}", reiOnly);
        
        // Register conditional JEI integration
        if (!reiOnly) {
            try {
                // Check if JEI classes are available before adding the integration
                Class.forName("mezz.jei.api.runtime.IJeiRuntime");
                integrations.add(new JEIIntegration());
                EnoughFolders.LOGGER.info("JEI integration enabled");
            } catch (ClassNotFoundException e) {
                EnoughFolders.LOGGER.info("JEI not found, integration disabled");
            }
        } else {
            EnoughFolders.LOGGER.info("Running in REI-only mode, JEI integration disabled");
        }
        
        // Add FTB Library integration
        integrations.add(new FTBLibraryIntegration());
        
        // Register conditional REI integration 
        if (!jeiOnly) {
            try {
                // Check if REI classes are available before adding the integration
                Class.forName("me.shedaniel.rei.api.client.REIRuntime");
                integrations.add(new REIIntegration());
                if (reiOnly) {
                    EnoughFolders.LOGGER.info("EnoughFolders: Running in REI-only mode");
                }
                EnoughFolders.LOGGER.info("REI integration enabled");
            } catch (ClassNotFoundException e) {
                EnoughFolders.LOGGER.info("REI not found, integration disabled");
            }
        } else {
            EnoughFolders.LOGGER.info("Running in JEI-only mode, REI integration disabled");
        }
        
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
     * Get an integration by class name, using reflection.
     * This is useful when you want to avoid direct class dependencies.
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
        // Make sure integrations are initialized
        if (!initialized) {
            initialize();
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
