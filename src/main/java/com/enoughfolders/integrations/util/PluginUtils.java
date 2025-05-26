package com.enoughfolders.integrations.util;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.util.DebugLogger;

import java.util.function.Supplier;

/**
 * Utility class for common plugin operations across all integrations.
 */
public final class PluginUtils {

    private PluginUtils() {
        // Utility class should not be instantiated
    }

    /**
     * Safely executes a plugin registration operation with proper error handling.
     * 
     * @param integrationName The name of the integration for logging
     * @param operation The registration operation to execute
     * @return true if registration was successful, false otherwise
     */
    public static boolean safePluginRegistration(String integrationName, Runnable operation) {
        try {
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Starting plugin registration for {}", integrationName);
            
            operation.run();
            
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Successfully registered {} plugin", integrationName);
            return true;
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to register {} plugin", integrationName, e);
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Plugin registration failed for {}: {}", integrationName, e.getMessage());
            return false;
        }
    }

    /**
     * Safely executes a plugin initialization operation with proper error handling.
     * 
     * @param integrationName The name of the integration for logging
     * @param operation The initialization operation to execute
     * @return true if initialization was successful, false otherwise
     */
    public static boolean safePluginInitialization(String integrationName, Runnable operation) {
        try {
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Starting plugin initialization for {}", integrationName);
            
            operation.run();
            
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Successfully initialized {} plugin", integrationName);
            return true;
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to initialize {} plugin", integrationName, e);
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Plugin initialization failed for {}: {}", integrationName, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a plugin class is available in the classpath.
     * 
     * @param className The fully qualified class name to check
     * @return true if the class is available, false otherwise
     */
    public static boolean isPluginClassAvailable(String className) {
        try {
            Class.forName(className);
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Plugin class found: {}", className);
            return true;
        } catch (ClassNotFoundException e) {
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Plugin class not found: {}", className);
            return false;
        }
    }

    /**
     * Validates plugin dependencies are available before registration.
     * 
     * @param integrationName The name of the integration
     * @param requiredClasses Array of required class names
     * @return true if all dependencies are available, false otherwise
     */
    public static boolean validatePluginDependencies(String integrationName, String... requiredClasses) {
        for (String className : requiredClasses) {
            if (!isPluginClassAvailable(className)) {
                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                    "{} plugin dependency not available: {}", integrationName, className);
                return false;
            }
        }
        
        DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
            "All {} plugin dependencies are available", integrationName);
        return true;
    }

    /**
     * Executes a plugin operation with retry logic for transient failures.
     * 
     * @param integrationName The name of the integration for logging
     * @param operation The operation to execute (should return true on success)
     * @param maxRetries Maximum number of retry attempts
     * @param retryDelayMs Delay between retry attempts in milliseconds
     * @return true if operation succeeded within retry limit, false otherwise
     */
    public static boolean executeWithRetry(String integrationName, Supplier<Boolean> operation, 
                                         int maxRetries, long retryDelayMs) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (operation.get()) {
                    if (attempt > 1) {
                        DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                            "{} operation succeeded on attempt {}", integrationName, attempt);
                    }
                    return true;
                }
            } catch (Exception e) {
                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                    "{} operation failed on attempt {}: {}", integrationName, attempt, e.getMessage());
                
                if (attempt == maxRetries) {
                    EnoughFolders.LOGGER.error("Final attempt failed for {} operation", integrationName, e);
                    break;
                }
            }
            
            // Wait before retrying (except on last attempt)
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                        "{} operation interrupted during retry delay", integrationName);
                    break;
                }
            }
        }
        
        return false;
    }

    /**
     * Safely shuts down a plugin with proper cleanup.
     * 
     * @param integrationName The name of the integration for logging
     * @param shutdownOperation The shutdown operation to execute
     */
    public static void safePluginShutdown(String integrationName, Runnable shutdownOperation) {
        try {
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Starting plugin shutdown for {}", integrationName);
            
            shutdownOperation.run();
            
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Successfully shut down {} plugin", integrationName);
                
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error during {} plugin shutdown", integrationName, e);
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Plugin shutdown error for {}: {}", integrationName, e.getMessage());
        }
    }

    /**
     * Measures and logs the execution time of a plugin operation.
     * 
     * @param integrationName The name of the integration for logging
     * @param operationName The name of the operation being measured
     * @param operation The operation to execute and measure
     * @return true if the operation completed successfully, false otherwise
     */
    public static boolean measurePluginOperation(String integrationName, String operationName, 
                                               Supplier<Boolean> operation) {
        long startTime = System.currentTimeMillis();
        
        try {
            boolean result = operation.get();
            long duration = System.currentTimeMillis() - startTime;
            
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "{} {} operation completed in {}ms with result: {}", 
                integrationName, operationName, duration, result);
                
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "{} {} operation failed after {}ms: {}", 
                integrationName, operationName, duration, e.getMessage());
                
            return false;
        }
    }
    
    /**
     * Safely invokes a method on an object if the method exists, with proper error handling.
     * 
     * @param target The object to invoke the method on
     * @param methodName The name of the method to invoke
     * @return true if the method was successfully invoked, false if the method doesn't exist or invocation failed
     */
    public static boolean invokeIfPresent(Object target, String methodName) {
        if (target == null || methodName == null) {
            return false;
        }
        
        try {
            // Get the method with no parameters
            java.lang.reflect.Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
            
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Successfully invoked method {} on {}", 
                methodName, target.getClass().getSimpleName());
            return true;
            
        } catch (NoSuchMethodException e) {
            // Method doesn't exist - this is expected in many cases
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Method {} not found on {} (this is normal)", 
                methodName, target.getClass().getSimpleName());
            return false;
            
        } catch (Exception e) {
            // Other reflection errors
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Failed to invoke method {} on {}: {}", 
                methodName, target.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Safely invokes a method on an object with parameters if the method exists.
     * 
     * @param target The object to invoke the method on
     * @param methodName The name of the method to invoke
     * @param parameterTypes The parameter types for method resolution
     * @param arguments The arguments to pass to the method
     * @return true if the method was successfully invoked, false if the method doesn't exist or invocation failed
     */
    public static boolean invokeIfPresent(Object target, String methodName, Class<?>[] parameterTypes, Object... arguments) {
        if (target == null || methodName == null) {
            return false;
        }
        
        try {
            // Get the method with specified parameters
            java.lang.reflect.Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            method.invoke(target, arguments);
            
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Successfully invoked method {} on {} with {} parameters", 
                methodName, target.getClass().getSimpleName(), parameterTypes.length);
            return true;
            
        } catch (NoSuchMethodException e) {
            // Method doesn't exist - this is expected in many cases
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Method {} with {} parameters not found on {} (this is normal)", 
                methodName, parameterTypes.length, target.getClass().getSimpleName());
            return false;
            
        } catch (Exception e) {
            // Other reflection errors
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Failed to invoke method {} on {}: {}", 
                methodName, target.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Creates a standardized error handler for integration operations.
     * 
     * @param integrationName The name of the integration for context
     * @return A Consumer that handles errors with appropriate logging and recovery
     */
    public static java.util.function.Consumer<Exception> createErrorHandler(String integrationName) {
        return (Exception e) -> {
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Error in {} integration: {}", 
                integrationName, e.getMessage());
        };
    }
}
