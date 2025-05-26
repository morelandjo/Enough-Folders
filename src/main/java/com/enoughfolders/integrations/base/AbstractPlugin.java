package com.enoughfolders.integrations.base;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.util.DebugLogger;

/**
 * Abstract base class for all integration plugins.
 */
public abstract class AbstractPlugin {
    
    /**
     * The display name for this plugin
     */
    protected final String pluginName;
    
    /**
     * Flag to track if this plugin has been registered
     */
    protected boolean registered = false;
    
    /**
     * Creates a new abstract plugin.
     *
     * @param pluginName The display name for this plugin
     */
    protected AbstractPlugin(String pluginName) {
        this.pluginName = pluginName;
        logInfo("Plugin constructor called");
    }
    
    /**
     * Registers the plugin.
     */
    public void register() {
        if (registered) {
            logDebug("Plugin already registered, skipping");
            return;
        }
        
        try {
            logInfo("Registering plugin");
            
            // Perform plugin-specific registration
            doRegister();
            
            registered = true;
            logInfo("Plugin registration complete");
            
        } catch (Exception e) {
            logError("Failed to register plugin: {}", e.getMessage(), e);
            throw new RuntimeException("Plugin registration failed", e);
        }
    }
    
    /**
     * Performs the actual registration logic specific to each plugin.
     */
    protected abstract void doRegister();
    
    /**
     * Gets the plugin name.
     *
     * @return The plugin name
     */
    public String getPluginName() {
        return pluginName;
    }
    
    /**
     * Checks if this plugin has been registered.
     *
     * @return true if registered, false otherwise
     */
    public boolean isRegistered() {
        return registered;
    }
    
    /**
     * Safely executes a plugin operation that might throw an exception.
     *
     * @param operation The operation to execute
     * @param operationName The name of the operation for logging
     */
    protected void safeExecute(SafePluginOperation operation, String operationName) {
        try {
            operation.execute();
        } catch (Exception e) {
            logError("Error during {}: {}", operationName, e.getMessage(), e);
        }
    }
    
    /**
     * Logs an info message with plugin context.
     *
     * @param message The message format
     * @param args The message arguments
     */
    protected void logInfo(String message, Object... args) {
        String formattedMessage = String.format("[%s] %s", pluginName, message);
        EnoughFolders.LOGGER.info(formattedMessage, args);
    }
    
    /**
     * Logs a debug message with plugin context.
     *
     * @param message The message format
     * @param args The message arguments
     */
    protected void logDebug(String message, Object... args) {
        String formattedMessage = String.format("[%s] %s", pluginName, message);
        EnoughFolders.LOGGER.debug(formattedMessage, args);
        
        // Also log to DebugLogger if available
        DebugLogger.debugValue(
            DebugLogger.Category.INTEGRATION,
            formattedMessage, 
            args.length > 0 ? args[0] : ""
        );
    }
    
    /**
     * Logs an error message with plugin context.
     *
     * @param message The message format
     * @param args The message arguments (last argument should be exception if present)
     */
    protected void logError(String message, Object... args) {
        String formattedMessage = String.format("[%s] %s", pluginName, message);
        
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
     * Checks if a class exists by name.
     *
     * @param className The fully qualified class name to check
     * @return true if the class exists, false otherwise
     */
    protected boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Template method for plugin initialization that can be overridden
     * by subclasses that need initialization logic.
     */
    protected void initialize() {
        logDebug("Plugin initialization called (default implementation)");
    }
    
    /**
     * Template method for plugin cleanup that can be overridden
     * by subclasses that need cleanup logic.
     */
    protected void cleanup() {
        logDebug("Plugin cleanup called (default implementation)");
    }
    
    /**
     * Functional interface for safe plugin operations.
     */
    @FunctionalInterface
    protected interface SafePluginOperation {
        /**
         * Executes the plugin operation.
         *
         * @throws Exception If the operation fails
         */
        void execute() throws Exception;
    }
}
