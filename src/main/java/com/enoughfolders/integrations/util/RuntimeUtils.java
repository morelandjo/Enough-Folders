package com.enoughfolders.integrations.util;

import com.enoughfolders.integrations.base.AbstractIntegration;
import com.enoughfolders.integrations.base.AbstractRuntimeManager;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Utility class for managing runtime state and operations across different integrations.
 */
public class RuntimeUtils {
    private static final DebugLogger.Category LOGGER_CATEGORY = DebugLogger.Category.INTEGRATION;
    
    // Cache for runtime availability checks to avoid repeated expensive operations
    private static final ConcurrentMap<String, Boolean> runtimeAvailabilityCache = new ConcurrentHashMap<>();
    
    // Cache for reflection operations to improve performance
    private static final ConcurrentMap<String, Method> methodCache = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Field> fieldCache = new ConcurrentHashMap<>();
    
    private RuntimeUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Checks if a specific integration runtime is available and active.
     * 
     * @param integrationName The name of the integration (e.g., "JEI", "EMI", "REI")
     * @param runtimeManager The runtime manager to check
     * @return true if the runtime is available and active
     */
    public static boolean isRuntimeAvailable(String integrationName, AbstractRuntimeManager<?> runtimeManager) {
        if (integrationName == null || runtimeManager == null) {
            DebugLogger.debugValue(LOGGER_CATEGORY, "Runtime check failed - null parameters for integration: {}", integrationName);
            return false;
        }
        
        // Check cache first
        String cacheKey = integrationName + "_runtime";
        Boolean cached = runtimeAvailabilityCache.get(cacheKey);
        if (cached != null) {
            DebugLogger.debugValues(LOGGER_CATEGORY, "Runtime availability from cache for {}: {}", integrationName, cached);
            return cached;
        }
        
        try {
            boolean available = runtimeManager.hasRuntime();
            runtimeAvailabilityCache.put(cacheKey, available);
            DebugLogger.debugValues(LOGGER_CATEGORY, "Runtime availability checked for {}: {}", integrationName, available);
            return available;
        } catch (Exception e) {
            System.err.println("[RuntimeUtils] Failed to check runtime availability for " + integrationName + ": " + e.getMessage());
            runtimeAvailabilityCache.put(cacheKey, false);
            return false;
        }
    }
    
    /**
     * Safely retrieves the runtime instance from a runtime manager.
     * 
     * @param <T> The type of runtime object being retrieved
     * @param runtimeManager The runtime manager
     * @param integrationName The integration name for logging
     * @return Optional containing the runtime if available
     */
    public static <T> Optional<T> getRuntimeSafely(AbstractRuntimeManager<T> runtimeManager, String integrationName) {
        if (runtimeManager == null) {
            DebugLogger.debugValue(LOGGER_CATEGORY, "Cannot get runtime - null manager for integration: {}", integrationName);
            return Optional.empty();
        }
        
        try {
            Optional<T> runtime = runtimeManager.getRuntime();
            if (runtime.isPresent()) {
                DebugLogger.debugValue(LOGGER_CATEGORY, "Runtime retrieved successfully for integration: {}", integrationName);
                return runtime;
            } else {
                DebugLogger.debugValue(LOGGER_CATEGORY, "Runtime is null for integration: {}", integrationName);
                return Optional.empty();
            }
        } catch (Exception e) {
            System.err.println("[RuntimeUtils] Failed to get runtime for " + integrationName + ": " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Executes an operation with the runtime, handling exceptions gracefully.
     * 
     * @param <T> The type of runtime object
     * @param <R> The return type of the operation
     * @param runtimeManager The runtime manager
     * @param operation The operation to execute with the runtime
     * @param integrationName The integration name for logging
     * @param defaultValue The default value to return if operation fails
     * @return The result of the operation or the default value
     */
    public static <T, R> R withRuntime(AbstractRuntimeManager<T> runtimeManager, 
                                       RuntimeOperation<T, R> operation, 
                                       String integrationName, 
                                       R defaultValue) {
        Optional<T> runtime = getRuntimeSafely(runtimeManager, integrationName);
        if (runtime.isPresent()) {
            try {
                R result = operation.execute(runtime.get());
                DebugLogger.debugValue(LOGGER_CATEGORY, "Runtime operation completed for integration: {}", integrationName);
                return result;
            } catch (Exception e) {
                System.err.println("[RuntimeUtils] Runtime operation failed for " + integrationName + ": " + e.getMessage());
            }
        }
        return defaultValue;
    }
    
    /**
     * Functional interface for runtime operations.
     * 
     * @param <T> The type of runtime object this operation works with
     * @param <R> The return type of the operation
     */
    @FunctionalInterface
    public interface RuntimeOperation<T, R> {
        /**
         * Executes the operation with the given runtime.
         * 
         * @param runtime The runtime object to operate on
         * @return The result of the operation
         * @throws Exception If the operation fails
         */
        R execute(T runtime) throws Exception;
    }
    
    /**
     * Clears the runtime availability cache for a specific integration.
     * 
     * @param integrationName The integration name to clear cache for
     */
    public static void clearRuntimeCache(String integrationName) {
        if (integrationName != null) {
            String cacheKey = integrationName + "_runtime";
            runtimeAvailabilityCache.remove(cacheKey);
            DebugLogger.debugValue(LOGGER_CATEGORY, "Runtime cache cleared for integration: {}", integrationName);
        }
    }
    
    /**
     * Clears all runtime caches. Useful for complete state reset.
     */
    public static void clearAllRuntimeCaches() {
        runtimeAvailabilityCache.clear();
        methodCache.clear();
        fieldCache.clear();
        DebugLogger.debug(LOGGER_CATEGORY, "All runtime caches cleared");
    }
    
    /**
     * Safely retrieves the current player from Minecraft client.
     * 
     * @return Optional containing the player if available
     */
    public static Optional<Player> getCurrentPlayer() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.player != null) {
                return Optional.of(minecraft.player);
            }
        } catch (Exception e) {
            System.err.println("[RuntimeUtils] Failed to get current player: " + e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Safely retrieves the current screen from Minecraft client.
     * 
     * @return Optional containing the screen if available
     */
    public static Optional<Screen> getCurrentScreen() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.screen != null) {
                return Optional.of(minecraft.screen);
            }
        } catch (Exception e) {
            System.err.println("[RuntimeUtils] Failed to get current screen: " + e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Checks if the current environment is a client-side context.
     * 
     * @return true if running on client side
     */
    public static boolean isClientSide() {
        try {
            return getCurrentPlayer()
                .map(player -> player.level().isClientSide)
                .orElse(true); // Default to true for safety
        } catch (Exception e) {
            System.err.println("[RuntimeUtils] Failed to check client side: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * Safely invokes a method using reflection with caching.
     * 
     * @param target The target object
     * @param methodName The method name
     * @param parameters The method parameters
     * @return Optional containing the result
     */
    public static Optional<Object> invokeMethodSafely(Object target, String methodName, Object... parameters) {
        if (target == null || methodName == null) {
            return Optional.empty();
        }
        
        String cacheKey = target.getClass().getName() + "." + methodName;
        Method method = methodCache.get(cacheKey);
        
        if (method == null) {
            try {
                Class<?>[] parameterTypes = new Class<?>[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    parameterTypes[i] = parameters[i] != null ? parameters[i].getClass() : Object.class;
                }
                method = target.getClass().getMethod(methodName, parameterTypes);
                method.setAccessible(true);
                methodCache.put(cacheKey, method);
            } catch (Exception e) {
                System.err.println("[RuntimeUtils] Failed to find method: " + methodName + " - " + e.getMessage());
                return Optional.empty();
            }
        }
        
        try {
            Object result = method.invoke(target, parameters);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            System.err.println("[RuntimeUtils] Failed to invoke method: " + methodName + " - " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Safely retrieves a field value using reflection with caching.
     * 
     * @param target The target object
     * @param fieldName The field name
     * @return Optional containing the field value
     */
    public static Optional<Object> getFieldSafely(Object target, String fieldName) {
        if (target == null || fieldName == null) {
            return Optional.empty();
        }
        
        String cacheKey = target.getClass().getName() + "." + fieldName;
        Field field = fieldCache.get(cacheKey);
        
        if (field == null) {
            try {
                field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                fieldCache.put(cacheKey, field);
            } catch (Exception e) {
                System.err.println("[RuntimeUtils] Failed to find field: " + fieldName + " - " + e.getMessage());
                return Optional.empty();
            }
        }
        
        try {
            Object value = field.get(target);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            System.err.println("[RuntimeUtils] Failed to get field value: " + fieldName + " - " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Measures the execution time of an operation.
     * 
     * @param <T> The return type of the operation
     * @param operation The operation to measure
     * @param operationName The name for logging
     * @return The result of the operation
     */
    public static <T> T measureExecutionTime(Supplier<T> operation, String operationName) {
        long startTime = System.nanoTime();
        try {
            T result = operation.get();
            long duration = System.nanoTime() - startTime;
            DebugLogger.debugValues(LOGGER_CATEGORY, "Operation {} completed in {}ns", operationName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.nanoTime() - startTime;
            System.err.println("[RuntimeUtils] Operation failed after " + duration + "ns: " + operationName + " - " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Creates a safe wrapper for integration operations that handles common exceptions.
     * 
     * @param <T> The type of integration
     * @param <R> The return type of the operation
     * @param integration The integration to wrap
     * @param operation The operation to execute
     * @param defaultValue The default value if operation fails
     * @return The result or default value
     */
    public static <T extends AbstractIntegration, R> R safeIntegrationOperation(
            T integration, 
            IntegrationOperation<T, R> operation, 
            R defaultValue) {
        if (integration == null || !integration.isAvailable()) {
            DebugLogger.debug(LOGGER_CATEGORY, "Integration not available for operation");
            return defaultValue;
        }
        
        try {
            return operation.execute(integration);
        } catch (Exception e) {
            System.err.println("[RuntimeUtils] Integration operation failed: " + e.getMessage());
            return defaultValue;
        }
    }
    
    /**
     * Functional interface for integration operations.
     * 
     * @param <T> The type of integration this operation works with
     * @param <R> The return type of the operation
     */
    @FunctionalInterface
    public interface IntegrationOperation<T extends AbstractIntegration, R> {
        /**
         * Executes the operation with the given integration.
         * 
         * @param integration The integration object to operate on
         * @return The result of the operation
         * @throws Exception If the operation fails
         */
        R execute(T integration) throws Exception;
    }
    
    /**
     * Validates the current runtime environment for integration compatibility.
     * 
     * @return true if the runtime environment is suitable for integrations, false otherwise
     */
    public static boolean validateRuntimeEnvironment() {
        try {
            // Check for required Minecraft classes
            Class.forName("net.minecraft.client.Minecraft");
            
            // Check for required NeoForge classes
            Class.forName("net.neoforged.neoforge.common.NeoForge");
            
            // Check for basic reflection capabilities
            if (!hasReflectionCapabilities()) {
                DebugLogger.debugValue(LOGGER_CATEGORY, "Runtime validation failed: {}", "Insufficient reflection capabilities");
                return false;
            }
            
            DebugLogger.debugValue(LOGGER_CATEGORY, "Runtime environment validation: {}", "PASSED");
            return true;
            
        } catch (ClassNotFoundException e) {
            DebugLogger.debugValue(LOGGER_CATEGORY, "Runtime validation failed - missing class: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            DebugLogger.debugValue(LOGGER_CATEGORY, "Runtime validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if the current runtime has sufficient reflection capabilities.
     * 
     * @return true if reflection capabilities are available, false otherwise
     */
    private static boolean hasReflectionCapabilities() {
        try {
            // Test basic reflection operations
            Class<?> testClass = Object.class;
            testClass.getDeclaredMethods();
            testClass.getDeclaredFields();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
