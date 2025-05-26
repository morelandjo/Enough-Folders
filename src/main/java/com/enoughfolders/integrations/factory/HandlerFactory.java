package com.enoughfolders.integrations.factory;

import com.enoughfolders.integrations.common.handlers.BaseAddToFolderHandler;
import com.enoughfolders.integrations.common.handlers.BaseFolderScreenHandler;
import com.enoughfolders.integrations.common.handlers.BaseRecipeGuiHandler;
import com.enoughfolders.integrations.util.PluginUtils;
import com.enoughfolders.util.DebugLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Centralized factory for creating and managing integration handlers.
 */
public class HandlerFactory {
        
    // Handler type enumeration for type-safe creation
    public enum HandlerType {
        /** Handler for adding ingredients to folders */
        ADD_TO_FOLDER("AddToFolder"),
        /** Handler for folder screen integration */
        FOLDER_SCREEN("FolderScreen"),
        /** Handler for recipe GUI integration */
        RECIPE_GUI("RecipeGui");
        
        private final String displayName;
        
        /**
         * Constructor for HandlerType enum.
         *
         * @param displayName The display name for this handler type
         */
        HandlerType(String displayName) {
            this.displayName = displayName;
        }
        
        /**
         * Gets the display name for this handler type.
         *
         * @return The display name
         */
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Handler cache for singleton management
     */
    private static final Map<String, Object> handlerCache = new ConcurrentHashMap<>();
    
    /**
     * Handler factories registry
     */
    private static final Map<HandlerFactoryKey, Supplier<?>> handlerFactories = new ConcurrentHashMap<>();
    
    /**
     * Handler dependencies registry
     */
    private static final Map<Class<?>, Set<Class<?>>> handlerDependencies = new ConcurrentHashMap<>();
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private HandlerFactory() {
        // Utility class should not be instantiated
    }
    
    /**
     * Key for handler factory registration combining integration type and handler type
     */
    public static class HandlerFactoryKey {
        private final IntegrationFactory.IntegrationType integrationType;
        private final HandlerType handlerType;
        
        /**
         * Creates a new handler factory key.
         *
         * @param integrationType The integration type
         * @param handlerType The handler type
         */
        public HandlerFactoryKey(IntegrationFactory.IntegrationType integrationType, HandlerType handlerType) {
            this.integrationType = integrationType;
            this.handlerType = handlerType;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HandlerFactoryKey that = (HandlerFactoryKey) o;
            return integrationType == that.integrationType && handlerType == that.handlerType;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(integrationType, handlerType);
        }
        
        @Override
        public String toString() {
            return integrationType + ":" + handlerType;
        }
    }
    
    /**
     * Register a handler factory for a specific integration and handler type
     *
     * @param integrationType The integration type to register for
     * @param handlerType The handler type to register for
     * @param factory The factory supplier to register
     * @param <T> The handler type
     */
    public static <T> void registerHandlerFactory(IntegrationFactory.IntegrationType integrationType,
                                                  HandlerType handlerType,
                                                  Supplier<T> factory) {
        HandlerFactoryKey key = new HandlerFactoryKey(integrationType, handlerType);
        handlerFactories.put(key, factory);
        DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
            "Registered handler factory for {}", key.toString());
    }
    
    /**
     * Register handler dependencies for proper initialization order
     *
     * @param handlerClass The handler class to register dependencies for
     * @param dependencies The classes this handler depends on
     */
    public static void registerHandlerDependencies(Class<?> handlerClass, Class<?>... dependencies) {
        handlerDependencies.put(handlerClass, Set.of(dependencies));
        DebugLogger.debugValues(DebugLogger.Category.INTEGRATION,
            "Registered dependencies for handler {} with dependencies {}", 
            handlerClass.getSimpleName(), Arrays.toString(dependencies));
    }
    
    /**
     * Create a handler instance with type safety
     *
     * @param integrationType The integration type to create handler for
     * @param handlerType The type of handler to create
     * @param expectedType The expected class type of the handler
     * @param <T> The handler type
     * @return The created handler instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T createHandler(IntegrationFactory.IntegrationType integrationType,
                                     HandlerType handlerType,
                                     Class<T> expectedType) {
        HandlerFactoryKey key = new HandlerFactoryKey(integrationType, handlerType);
        String cacheKey = key.toString();
        
        try {
            // Check cache first
            Object cached = handlerCache.get(cacheKey);
            if (cached != null) {
                if (expectedType.isInstance(cached)) {
                    DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                        "Retrieved cached handler {}", cacheKey);
                    return expectedType.cast(cached);
                } else {
                    DebugLogger.debug(DebugLogger.Category.INTEGRATION,
                        "Cached handler type mismatch for key: " + cacheKey + 
                        ", expected: " + expectedType.getSimpleName() + 
                        ", actual: " + cached.getClass().getSimpleName());
                    handlerCache.remove(cacheKey);
                }
            }
            
            // Get factory
            Supplier<?> factory = handlerFactories.get(key);
            if (factory == null) {
                throw new IllegalArgumentException("No handler factory registered for: " + key);
            }
            
            // Create instance
            Object instance = factory.get();
            if (instance == null) {
                throw new RuntimeException("Handler factory returned null for: " + key);
            }
            
            // Validate type
            if (!expectedType.isInstance(instance)) {
                throw new ClassCastException("Handler factory created wrong type for: " + key + 
                                           ", expected: " + expectedType.getSimpleName() + 
                                           ", actual: " + instance.getClass().getSimpleName());
            }
            
            // Initialize if needed
            initializeHandler(instance);
            
            // Cache instance
            handlerCache.put(cacheKey, instance);
            
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION,
                "Created and cached handler {} of type {}", 
                cacheKey, instance.getClass().getSimpleName());
            return expectedType.cast(instance);
            
        } catch (Exception e) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, 
                "Failed to create handler for: " + key + " - " + e.getMessage());
            throw new RuntimeException("Handler creation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create multiple handlers in batch with dependency resolution
     *
     * @param integrationType The integration type to create handlers for
     * @param handlerTypes The types of handlers to create
     * @return Map of handler type to created handler instance
     */
    public static Map<HandlerType, Object> createHandlers(IntegrationFactory.IntegrationType integrationType,
                                                          HandlerType... handlerTypes) {
        Map<HandlerType, Object> handlers = new LinkedHashMap<>();
        
        try {
            // Sort by dependencies
            List<HandlerType> sortedTypes = resolveDependencyOrder(handlerTypes);
            
            for (HandlerType handlerType : sortedTypes) {
                Object handler = createHandlerGeneric(integrationType, handlerType);
                if (handler != null) {
                    handlers.put(handlerType, handler);
                }
            }
            
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION,
                "Created handler batch for {} with {} handlers", 
                integrationType.toString(), handlers.size());
            return handlers;
            
        } catch (Exception e) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION,
                "Failed to create handler batch for: " + integrationType + " - " + e.getMessage());
            throw new RuntimeException("Batch handler creation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create handler with generic return type
     *
     * @param integrationType The integration type
     * @param handlerType The handler type
     * @return The created handler instance
     */
    private static Object createHandlerGeneric(IntegrationFactory.IntegrationType integrationType,
                                              HandlerType handlerType) {
        return switch (handlerType) {
            case ADD_TO_FOLDER -> createHandler(integrationType, handlerType, BaseAddToFolderHandler.class);
            case FOLDER_SCREEN -> createHandler(integrationType, handlerType, BaseFolderScreenHandler.class);
            case RECIPE_GUI -> createHandler(integrationType, handlerType, BaseRecipeGuiHandler.class);
        };
    }
    
    /**
     * Initialize handler if it has initialization methods
     *
     * @param handler The handler to initialize
     */
    private static void initializeHandler(Object handler) {
        try {
            // Try common initialization patterns
            PluginUtils.invokeIfPresent(handler, "initialize");
            PluginUtils.invokeIfPresent(handler, "onInit");
            PluginUtils.invokeIfPresent(handler, "setup");
            
        } catch (Exception e) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION,
                "Handler initialization failed for: " + handler.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
    
    /**
     * Resolve dependency order for handlers
     *
     * @param handlerTypes The handler types to resolve dependencies for
     * @return List of handler types in dependency order
     */
    private static List<HandlerType> resolveDependencyOrder(HandlerType[] handlerTypes) {
        List<HandlerType> sorted = new ArrayList<>();
        Set<HandlerType> processing = new HashSet<>();
        Set<HandlerType> processed = new HashSet<>();
        
        for (HandlerType type : handlerTypes) {
            if (!processed.contains(type)) {
                resolveDependencies(type, sorted, processing, processed);
            }
        }
        
        return sorted;
    }
    
    /**
     * Recursive dependency resolution
     *
     * @param type The handler type to resolve dependencies for
     * @param sorted The list to add resolved dependencies to
     * @param processing Set of types currently being processed
     * @param processed Set of types already processed
     */
    private static void resolveDependencies(HandlerType type,
                                          List<HandlerType> sorted,
                                          Set<HandlerType> processing,
                                          Set<HandlerType> processed) {
        if (processing.contains(type)) {
            throw new IllegalStateException("Circular dependency detected: " + type);
        }
        
        if (processed.contains(type)) {
            return;
        }
        
        processing.add(type);
        
        // Process dependencies first
        switch (type) {
            case RECIPE_GUI -> {
                if (!processed.contains(HandlerType.FOLDER_SCREEN)) {
                    resolveDependencies(HandlerType.FOLDER_SCREEN, sorted, processing, processed);
                }
            }
            case ADD_TO_FOLDER -> {
                if (!processed.contains(HandlerType.FOLDER_SCREEN)) {
                    resolveDependencies(HandlerType.FOLDER_SCREEN, sorted, processing, processed);
                }
            }
            case FOLDER_SCREEN -> {
                // No dependencies for folder screen handler
            }
        }
        
        processing.remove(type);
        processed.add(type);
        sorted.add(type);
    }
    
    /**
     * Get cached handler if available
     *
     * @param integrationType The integration type
     * @param handlerType The handler type
     * @param expectedType The expected class type
     * @param <T> The handler type
     * @return Optional containing the cached handler if available
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getCachedHandler(IntegrationFactory.IntegrationType integrationType,
                                                  HandlerType handlerType,
                                                  Class<T> expectedType) {
        HandlerFactoryKey key = new HandlerFactoryKey(integrationType, handlerType);
        Object cached = handlerCache.get(key.toString());
        
        if (cached != null && expectedType.isInstance(cached)) {
            return Optional.of(expectedType.cast(cached));
        }
        
        return Optional.empty();
    }
    
    /**
     * Clear handler cache for specific integration
     *
     * @param integrationType The integration type to clear cache for
     */
    public static void clearHandlerCache(IntegrationFactory.IntegrationType integrationType) {
        handlerCache.entrySet().removeIf(entry -> entry.getKey().startsWith(integrationType.toString()));
        DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
            "Cleared handler cache for integration {}", integrationType.toString());
    }
    
    /**
     * Clear all handler caches
     */
    public static void clearAllHandlerCaches() {
        handlerCache.clear();
        DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
            "Cleared all handler caches, count was {}", handlerCache.size());
    }
    
    /**
     * Get handler creation statistics
     *
     * @return Map containing various statistics about handler creation and caching
     */
    public static Map<String, Object> getHandlerStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedHandlers", handlerCache.size());
        stats.put("registeredFactories", handlerFactories.size());
        stats.put("registeredDependencies", handlerDependencies.size());
        
        Map<String, Integer> typeStats = new HashMap<>();
        for (String key : handlerCache.keySet()) {
            String type = key.split(":")[1];
            typeStats.merge(type, 1, Integer::sum);
        }
        stats.put("handlersByType", typeStats);
        
        return stats;
    }
    
    /**
     * Validate handler factory configuration
     *
     * @return true if configuration is valid, false otherwise
     */
    public static boolean validateConfiguration() {
        boolean valid = true;
        
        try {
            // Check that all integration types have handler factories
            for (IntegrationFactory.IntegrationType integrationType : IntegrationFactory.IntegrationType.values()) {
                for (HandlerType handlerType : HandlerType.values()) {
                    HandlerFactoryKey key = new HandlerFactoryKey(integrationType, handlerType);
                    if (!handlerFactories.containsKey(key)) {
                        DebugLogger.debug(DebugLogger.Category.INTEGRATION,
                            "Missing handler factory for: " + key);
                        valid = false;
                    }
                }
            }
            
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION,
                "Handler factory configuration validation {}", valid ? "PASSED" : "FAILED");
            return valid;
            
        } catch (Exception e) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION,
                "Handler factory validation failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Register default handler factories for all integration types
     */
    public static void registerDefaultFactories() {
        DebugLogger.debugValue(DebugLogger.Category.INTEGRATION,
            "Default handler factories registration {}", "initiated");
        
        try {
            // Register JEI factories
            registerJEIFactories();
            
            // Register EMI factories
            registerEMIFactories();
            
            // Register REI factories
            registerREIFactories();
            
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION,
                "All default handler factories registered successfully", "");
                
        } catch (Exception e) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION,
                "Failed to register default handler factories: " + e.getMessage());
        }
    }
    
    /**
     * Register JEI-specific handler factories
     */
    private static void registerJEIFactories() {
        try {
            // Register AddToFolder handler
            registerHandlerFactory(
                IntegrationFactory.IntegrationType.JEI,
                HandlerType.ADD_TO_FOLDER,
                () -> {
                    try {
                        Class<?> handlerClass = Class.forName("com.enoughfolders.integrations.jei.handlers.JEIAddToFolderHandler");
                        return handlerClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create JEI AddToFolder handler", e);
                    }
                }
            );
            
            // Register FolderScreen handler
            registerHandlerFactory(
                IntegrationFactory.IntegrationType.JEI,
                HandlerType.FOLDER_SCREEN,
                () -> {
                    try {
                        Class<?> handlerClass = Class.forName("com.enoughfolders.integrations.jei.gui.handlers.FolderScreenHandler");
                        return handlerClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create JEI FolderScreen handler", e);
                    }
                }
            );
            
            // Register RecipeGui handler
            registerHandlerFactory(
                IntegrationFactory.IntegrationType.JEI,
                HandlerType.RECIPE_GUI,
                () -> {
                    try {
                        Class<?> handlerClass = Class.forName("com.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler");
                        return handlerClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create JEI RecipeGui handler", e);
                    }
                }
            );
            
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION,
                "JEI handler factories registered", "");
                
        } catch (Exception e) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION,
                "Failed to register JEI factories: " + e.getMessage());
        }
    }
    
    /**
     * Register EMI-specific handler factories
     */
    private static void registerEMIFactories() {
        try {
            // Register AddToFolder handler
            registerHandlerFactory(
                IntegrationFactory.IntegrationType.EMI,
                HandlerType.ADD_TO_FOLDER,
                () -> {
                    try {
                        Class<?> handlerClass = Class.forName("com.enoughfolders.integrations.emi.handlers.EMIAddToFolderHandler");
                        return handlerClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create EMI AddToFolder handler", e);
                    }
                }
            );
            
            // Register FolderScreen handler (EMI uses the folder ingredient handler)
            registerHandlerFactory(
                IntegrationFactory.IntegrationType.EMI,
                HandlerType.FOLDER_SCREEN,
                () -> {
                    try {
                        Class<?> handlerClass = Class.forName("com.enoughfolders.integrations.emi.gui.handlers.EMIFolderIngredientHandler");
                        // EMI handler needs constructor parameters, will be created when needed
                        return handlerClass;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create EMI FolderScreen handler", e);
                    }
                }
            );
            
            // Register RecipeGui handler (placeholder - EMI handles this through plugin)
            registerHandlerFactory(
                IntegrationFactory.IntegrationType.EMI,
                HandlerType.RECIPE_GUI,
                () -> {
                    // EMI uses the plugin system for recipe GUI handling
                    return new Object(); // Placeholder
                }
            );
            
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION,
                "EMI handler factories registered", "");
                
        } catch (Exception e) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION,
                "Failed to register EMI factories: " + e.getMessage());
        }
    }
    
    /**
     * Register REI-specific handler factories
     */
    private static void registerREIFactories() {
        try {
            // Register AddToFolder handler (REI doesn't have one implemented yet)
            registerHandlerFactory(
                IntegrationFactory.IntegrationType.REI,
                HandlerType.ADD_TO_FOLDER,
                () -> {
                    // REI AddToFolder handler would be implemented here
                    return new Object(); // Placeholder
                }
            );
            
            // Register FolderScreen handler (REI uses plugin system)
            registerHandlerFactory(
                IntegrationFactory.IntegrationType.REI,
                HandlerType.FOLDER_SCREEN,
                () -> {
                    // REI uses exclusion zones through the plugin
                    return new Object(); // Placeholder
                }
            );
            
            // Register RecipeGui handler (REI uses plugin system)
            registerHandlerFactory(
                IntegrationFactory.IntegrationType.REI,
                HandlerType.RECIPE_GUI,
                () -> {
                    try {
                        Class<?> handlerClass = Class.forName("com.enoughfolders.integrations.rei.gui.handlers.REIRecipeGuiHandler");
                        return handlerClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create REI RecipeGui handler", e);
                    }
                }
            );
            
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION,
                "REI handler factories registered", "");
                
        } catch (Exception e) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION,
                "Failed to register REI factories: " + e.getMessage());
        }
    }
}
