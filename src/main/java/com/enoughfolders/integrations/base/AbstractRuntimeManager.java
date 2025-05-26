package com.enoughfolders.integrations.base;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.util.DebugLogger;

import java.util.Optional;

/**
 * Abstract base class for all runtime managers.
 *
 * @param <T> The type of runtime object this manager handles
 */
public abstract class AbstractRuntimeManager<T> {
    
    /**
     * The integration name for this runtime manager
     */
    protected final String integrationName;
    
    /**
     * The runtime instance
     */
    protected T runtime;
    
    /**
     * Flag to track if runtime is available
     */
    protected boolean runtimeAvailable = false;
    
    /**
     * Creates a new abstract runtime manager.
     *
     * @param integrationName The name of the integration this manager belongs to
     */
    protected AbstractRuntimeManager(String integrationName) {
        this.integrationName = integrationName;
    }
    
    /**
     * Sets the runtime instance.
     *
     * @param runtime The runtime instance to set
     */
    public void setRuntime(T runtime) {
        if (runtime == null) {
            logDebug("Runtime is null, clearing runtime");
            clearRuntime();
            return;
        }
        
        this.runtime = runtime;
        this.runtimeAvailable = true;
        
        logInfo("Runtime set successfully");
        
        // Perform any runtime-specific initialization
        safeExecute(
            () -> onRuntimeSet(runtime),
            "runtime initialization"
        );
    }
    
    /**
     * Called when the runtime is set.
     *
     * @param runtime The runtime that was set
     */
    protected void onRuntimeSet(T runtime) {
        logDebug("Runtime set (default handler)");
    }
    
    /**
     * Gets the runtime instance.
     *
     * @return Optional containing the runtime, or empty if not available
     */
    public Optional<T> getRuntime() {
        return runtimeAvailable ? Optional.ofNullable(runtime) : Optional.empty();
    }
    
    /**
     * Checks if the runtime is available.
     *
     * @return true if runtime is available, false otherwise
     */
    public boolean hasRuntime() {
        return runtimeAvailable && runtime != null;
    }
    
    /**
     * Clears the runtime instance.
     */
    public void clearRuntime() {
        if (hasRuntime()) {
            logInfo("Clearing runtime");
            
            // Perform any cleanup before clearing
            safeExecute(
                () -> onRuntimeCleared(),
                "runtime cleanup"
            );
        }
        
        this.runtime = null;
        this.runtimeAvailable = false;
    }
    
    /**
     * Called when the runtime is cleared.
     */
    protected void onRuntimeCleared() {
        logDebug("Runtime cleared (default handler)");
    }
    
    /**
     * Executes an operation that requires the runtime to be available.
     *
     * @param operation The operation to execute
     * @param operationName The name of the operation for logging
     * @param <R> The return type
     * @return Optional containing the result, or empty if runtime not available or operation failed
     */
    public <R> Optional<R> withRuntime(RuntimeOperation<T, R> operation, String operationName) {
        if (!hasRuntime()) {
            logDebug("Runtime not available for operation: {}", operationName);
            return Optional.empty();
        }
        
        try {
            R result = operation.execute(runtime);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            logError("Error during {} with runtime: {}", operationName, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Executes an operation that requires the runtime to be available (void return).
     *
     * @param operation The operation to execute
     * @param operationName The name of the operation for logging
     * @return true if operation was executed successfully, false otherwise
     */
    public boolean withRuntimeVoid(VoidRuntimeOperation<T> operation, String operationName) {
        if (!hasRuntime()) {
            logDebug("Runtime not available for operation: {}", operationName);
            return false;
        }
        
        try {
            operation.execute(runtime);
            return true;
        } catch (Exception e) {
            logError("Error during {} with runtime: {}", operationName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Checks if the runtime is of a specific type.
     *
     * @param runtimeClass The class to check against
     * @return true if runtime is of the specified type, false otherwise
     */
    public boolean isRuntimeType(Class<?> runtimeClass) {
        return hasRuntime() && runtimeClass.isInstance(runtime);
    }
    
    /**
     * Gets the runtime cast to a specific type.
     *
     * @param runtimeClass The class to cast to
     * @param <R> The type to cast to
     * @return Optional containing the cast runtime, or empty if not available or wrong type
     */
    public <R> Optional<R> getRuntimeAs(Class<R> runtimeClass) {
        if (hasRuntime() && runtimeClass.isInstance(runtime)) {
            return Optional.of(runtimeClass.cast(runtime));
        }
        return Optional.empty();
    }
    
    /**
     * Gets runtime information for debugging purposes.
     *
     * @return String containing runtime information
     */
    public String getRuntimeInfo() {
        if (!hasRuntime()) {
            return "No runtime available";
        }
        
        return String.format("Runtime: %s [available: %s]", 
            runtime.getClass().getSimpleName(), 
            runtimeAvailable);
    }
    
    /**
     * Safely executes an operation that might throw an exception.
     *
     * @param operation The operation to execute
     * @param operationName The name of the operation for logging
     */
    protected void safeExecute(SafeOperation operation, String operationName) {
        try {
            operation.execute();
        } catch (Exception e) {
            logError("Error during {}: {}", operationName, e.getMessage(), e);
        }
    }
    
    /**
     * Logs an info message with runtime manager context.
     *
     * @param message The message format
     * @param args The message arguments
     */
    protected void logInfo(String message, Object... args) {
        String formattedMessage = String.format("[%s RuntimeManager] %s", integrationName, message);
        EnoughFolders.LOGGER.info(formattedMessage, args);
    }
    
    /**
     * Logs a debug message with runtime manager context.
     *
     * @param message The message format
     * @param args The message arguments
     */
    protected void logDebug(String message, Object... args) {
        String formattedMessage = String.format("[%s RuntimeManager] %s", integrationName, message);
        EnoughFolders.LOGGER.debug(formattedMessage, args);
        
        // Also log to DebugLogger if available
        DebugLogger.debugValue(
            DebugLogger.Category.INTEGRATION,
            formattedMessage, 
            args.length > 0 ? args[0] : ""
        );
    }
    
    /**
     * Logs an error message with runtime manager context.
     *
     * @param message The message format
     * @param args The message arguments (last argument should be exception if present)
     */
    protected void logError(String message, Object... args) {
        String formattedMessage = String.format("[%s RuntimeManager] %s", integrationName, message);
        
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
     * Functional interface for operations that use the runtime and return a value.
     *
     * @param <T> The runtime type
     * @param <R> The return type
     */
    @FunctionalInterface
    public interface RuntimeOperation<T, R> {
        /**
         * Executes the operation with the provided runtime.
         *
         * @param runtime The runtime instance to use
         * @return The result of the operation
         * @throws Exception If the operation fails
         */
        R execute(T runtime) throws Exception;
    }
    
    /**
     * Functional interface for operations that use the runtime but don't return a value.
     *
     * @param <T> The runtime type
     */
    @FunctionalInterface
    public interface VoidRuntimeOperation<T> {
        /**
         * Executes the operation with the provided runtime.
         *
         * @param runtime The runtime instance to use
         * @throws Exception If the operation fails
         */
        void execute(T runtime) throws Exception;
    }
    
    /**
     * Functional interface for safe operations.
     */
    @FunctionalInterface
    protected interface SafeOperation {
        /**
         * Executes the operation.
         *
         * @throws Exception If the operation fails
         */
        void execute() throws Exception;
    }
}
