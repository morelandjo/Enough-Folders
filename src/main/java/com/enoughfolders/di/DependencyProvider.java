package com.enoughfolders.di;

import com.enoughfolders.EnoughFolders;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Dependency provider central registry
 */
public class DependencyProvider {
    
    private DependencyProvider() {
        // Utility class should not be instantiated
    }
    private static final Map<Class<?>, Supplier<?>> dependencies = new HashMap<>();
    private static final Map<Class<?>, Object> singletons = new HashMap<>();

    /**
     * Register a dependency factory that creates a new instance each time it's requested.
     *
     * @param <T> The type of the dependency
     * @param type The class object representing the dependency type
     * @param factory The factory function that creates instances of the dependency
     */
    public static <T> void register(Class<T> type, Supplier<T> factory) {
        dependencies.put(type, factory);
        EnoughFolders.LOGGER.debug("Registered dependency factory for: {}", type.getSimpleName());
    }

    /**
     * Register a singleton dependency that will be reused for all requests.
     *
     * @param <T> The type of the dependency
     * @param type The class object representing the dependency type
     * @param instance The singleton instance of the dependency
     */
    public static <T> void registerSingleton(Class<T> type, T instance) {
        singletons.put(type, instance);
        EnoughFolders.LOGGER.debug("Registered singleton for: {}", type.getSimpleName());
    }

    /**
     * Get a dependency instance by its type.
     *
     * @param <T> The type of the dependency
     * @param type The class object representing the dependency type
     * @return An Optional containing the dependency instance, or empty if the dependency is not registered
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> get(Class<T> type) {
        // First check if we have a singleton instance
        if (singletons.containsKey(type)) {
            return Optional.of((T) singletons.get(type));
        }
        
        // Otherwise try to create a new instance
        Supplier<?> factory = dependencies.get(type);
        if (factory != null) {
            return Optional.of((T) factory.get());
        }
        
        // If we can't find a factory, check if we have a factory for any supertype or interface
        for (Map.Entry<Class<?>, Supplier<?>> entry : dependencies.entrySet()) {
            if (type.isAssignableFrom(entry.getKey())) {
                return Optional.of((T) entry.getValue().get());
            }
        }
        
        return Optional.empty();
    }

    /**
     * Get a required dependency instance by its type.
     *
     * @param <T> The type of the dependency
     * @param type The class object representing the dependency type
     * @return The dependency instance
     * @throws DependencyNotFoundException If the dependency is not registered
     */
    public static <T> T getRequired(Class<T> type) {
        return get(type).orElseThrow(() -> 
            new DependencyNotFoundException("Required dependency not found: " + type.getName()));
    }
    
    /**
     * Check if a dependency is registered.
     *
     * @param <T> The type of the dependency
     * @param type The class object representing the dependency type
     * @return True if the dependency is registered, false otherwise
     */
    public static <T> boolean has(Class<T> type) {
        return singletons.containsKey(type) || dependencies.containsKey(type);
    }
    
    /**
     * Clear all registered dependencies.
     */
    public static void clear() {
        dependencies.clear();
        singletons.clear();
    }
    
    /**
     * Exception thrown when a requested dependency is not found in the registry.
     */
    public static class DependencyNotFoundException extends RuntimeException {
        /**
         * Constructs a new dependency not found exception with the specified message.
         * 
         * @param message The detail message explaining the exception
         */
        public DependencyNotFoundException(String message) {
            super(message);
        }
    }
}
