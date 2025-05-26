package com.enoughfolders.di;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.integrations.ModIntegration;
import com.enoughfolders.integrations.api.RecipeViewingIntegration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory for creating integration instances.
 */
public class IntegrationFactory {
    
    private IntegrationFactory() {
        // Utility class should not be instantiated
    }
    private static final Map<Class<?>, Supplier<?>> factories = new HashMap<>();

    /**
     * Register a factory for creating instances of a specific integration type.
     *
     * @param <T> The type of integration
     * @param type The class of the integration
     * @param factory The factory function for creating integration instances
     */
    public static <T extends ModIntegration> void registerFactory(Class<T> type, Supplier<T> factory) {
        factories.put(type, factory);
        EnoughFolders.LOGGER.debug("Registered integration factory for: {}", type.getSimpleName());
    }

    /**
     * Create an instance of the specified integration type.
     *
     * @param <T> The type of integration
     * @param type The class of the integration to create
     * @return A new instance of the specified integration type
     * @throws IllegalStateException If no factory is registered for the specified type
     */
    @SuppressWarnings("unchecked")
    public static <T extends ModIntegration> T create(Class<T> type) {
        Supplier<?> factory = factories.get(type);
        if (factory == null) {
            throw new IllegalStateException("No factory registered for integration type: " + type.getName());
        }

        T instance = (T) factory.get();
        EnoughFolders.LOGGER.debug("Created integration instance: {}", type.getSimpleName());

        // Initialize the integration
        instance.initialize();
        // Note: Drag and drop functionality has been removed
        instance.registerDragAndDrop();

        // Register the instance with the dependency provider
        DependencyProvider.registerSingleton(type, instance);

        return instance;
    }
    
    /**
     * Check if a factory is registered for the specified integration type.
     *
     * @param <T> The type of integration
     * @param type The class of the integration
     * @return true if a factory is registered, false otherwise
     */
    public static <T extends ModIntegration> boolean hasFactory(Class<T> type) {
        return factories.containsKey(type);
    }
    
    /**
     * Create a RecipeViewingIntegration from its ID.
     *
     * @param id The integration ID (e.g., "jei", "rei")
     * @return A RecipeViewingIntegration instance, or null if the ID is not recognized
     */
    @SuppressWarnings("unchecked")
    public static RecipeViewingIntegration createRecipeViewingIntegration(String id) {
        try {
            if ("jei".equals(id)) {
                Class<?> jeiClass = Class.forName("com.enoughfolders.integrations.jei.core.JEIIntegration");
                return (RecipeViewingIntegration) create((Class<? extends ModIntegration>) jeiClass);
            } else if ("rei".equals(id)) {
                Class<?> reiClass = Class.forName("com.enoughfolders.integrations.rei.core.REIIntegration");
                return (RecipeViewingIntegration) create((Class<? extends ModIntegration>) reiClass);
            }
        } catch (ClassNotFoundException | ClassCastException e) {
            EnoughFolders.LOGGER.error("Failed to create RecipeViewingIntegration for ID: {}", id, e);
        }
        return null;
    }
}
