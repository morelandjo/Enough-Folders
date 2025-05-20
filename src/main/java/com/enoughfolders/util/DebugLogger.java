package com.enoughfolders.util;

import com.enoughfolders.EnoughFolders;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom debug logger
 */
public class DebugLogger {
    private static Logger LOGGER;
    private static boolean isTestEnvironment = false;
    
    /**
     * Logging categories for organizing debug messages into functional areas.
     */
    public enum Category {
        /** Initialization and startup related logs */
        INITIALIZATION("init"),
        
        /** Rendering and visual element related logs */
        RENDERING("render"),
        
        /** Keyboard and mouse input related logs */
        INPUT("input"),
        
        /** Folder data management related logs */
        FOLDER_MANAGER("folderManager"),
        
        /** Mod integration related logs */
        INTEGRATION("integration"),
        
        /** GUI state tracking related logs */
        GUI_STATE("guiState"),
        
        /** Mouse position and interaction related logs */
        MOUSE("mouse"),
        
        /** JEI-specific integration related logs */
        JEI_INTEGRATION("jeiIntegration"),
        
        /** REI-specific integration related logs */
        REI_INTEGRATION("reiIntegration");
        
        private final String id;
        
        /**
         * Creates a new category with the specified ID.
         * 
         * @param id Short identifier for the category used in config files and log output
         */
        Category(String id) {
            this.id = id;
        }
        
        /**
         * Gets the string identifier for this category.
         * 
         * @return The category's ID string
         */
        public String getId() {
            return id;
        }
    }
    
    // Map to track which categories are enabled
    private static final Map<Category, Boolean> CATEGORY_ENABLED = new HashMap<>();
    
    // Map to store the last value of each log message to avoid repeating the same log
    private static final Map<String, String> LAST_LOG_VALUES = new HashMap<>();
    
    static {
        // Initialize all categories to disabled by default
        for (Category category : Category.values()) {
            CATEGORY_ENABLED.put(category, false);
        }
        
        // Try to initialize the logger, but handle the case when running in tests
        try {
            // Try to get the logger from EnoughFolders class
            try {
                LOGGER = EnoughFolders.LOGGER;
            } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
                // If that fails, try to create a new logger
                LOGGER = LogManager.getLogger(DebugLogger.class);
            }
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            // We're probably running in a test environment without Log4j
            isTestEnvironment = true;
            System.out.println("[DebugLogger] Running in test environment without Log4j");
        }
    }
    
    /**
     * Enables or disables a specific debug category.
     * 
     * @param category The category to enable/disable
     * @param enabled Whether to enable or disable the category
     */
    public static void setEnabled(Category category, boolean enabled) {
        CATEGORY_ENABLED.put(category, enabled);
        logInfo("DebugLogger: Category " + category.getId() + " is now " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Checks if a category is enabled.
     * 
     * @param category The category to check
     * @return True if the category is enabled, false otherwise
     */
    public static boolean isEnabled(Category category) {
        return CATEGORY_ENABLED.getOrDefault(category, false);
    }
    
    /**
     * Enables or disables all categories at once.
     * 
     * @param enabled Whether to enable or disable all categories
     */
    public static void setAllEnabled(boolean enabled) {
        for (Category category : Category.values()) {
            CATEGORY_ENABLED.put(category, enabled);
        }
        logInfo("DebugLogger: All categories are now " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Helper method to log info messages that works in both normal and test environments.
     * 
     * @param message The message to log at INFO level
     */
    private static void logInfo(String message) {
        if (isTestEnvironment) {
            System.out.println(message);
        } else if (LOGGER != null) {
            LOGGER.info(message);
        }
    }
    
    /**
     * Logs a debug message if the specified category is enabled.
     * 
     * @param category The category of the debug message
     * @param message The message to log
     */
    public static void debug(Category category, String message) {
        if (!isEnabled(category)) return;
        
        // Use a combination of the message pattern (without dynamic values) and the category as the key
        String key = category.getId() + "::" + message;
        
        // If this exact message was already logged, don't log it again
        if (message.equals(LAST_LOG_VALUES.get(key))) {
            return;
        }
        
        // Store this message as the last logged value for this key
        LAST_LOG_VALUES.put(key, message);
        
        // Output the debug message to console
        System.out.println("[Debug/" + category.getId() + "] " + message);
    }
    
    /**
     * Logs a debug message with a dynamic value that may change over time.
     * 
     * @param category The category of the debug message
     * @param messagePattern The static part of the message, optionally with {} placeholder
     * @param value The dynamic value that might change
     */
    public static void debugValue(Category category, String messagePattern, Object value) {
        if (!isEnabled(category)) return;
        
        // Convert value to string for comparison
        String valueStr = String.valueOf(value);
        
        // Use the message pattern without the value as the key
        String key = category.getId() + "::" + messagePattern;
        
        // If the value hasn't changed since last time, don't log it again
        if (valueStr.equals(LAST_LOG_VALUES.get(key))) {
            return;
        }
        
        // Store this value as the last logged value for this key
        LAST_LOG_VALUES.put(key, valueStr);
        
        // Output the debug message with the value
        String fullMessage = messagePattern.contains("{}") 
            ? messagePattern.replace("{}", valueStr) 
            : messagePattern + " " + valueStr;
            
        System.out.println("[Debug/" + category.getId() + "] " + fullMessage);
    }
    
    /**
     * Logs a debug message with multiple dynamic values.
     * 
     * @param category The category of the debug message
     * @param messagePattern The static part of the message with {} placeholders
     * @param values The dynamic values that might change
     */
    public static void debugValues(Category category, String messagePattern, Object... values) {
        if (!isEnabled(category)) return;
        
        // Build a combined string of all values for comparison
        StringBuilder valuesCombined = new StringBuilder();
        for (Object value : values) {
            valuesCombined.append(String.valueOf(value)).append("|");
        }
        String valuesStr = valuesCombined.toString();
        
        // Use the message pattern without the values as the key
        String key = category.getId() + "::" + messagePattern;
        
        // If the values haven't changed since last time, don't log it again
        if (valuesStr.equals(LAST_LOG_VALUES.get(key))) {
            return;
        }
        
        // Store these values as the last logged values for this key
        LAST_LOG_VALUES.put(key, valuesStr);
        
        // Replace placeholders with actual values
        String fullMessage = messagePattern;
        for (Object value : values) {
            fullMessage = fullMessage.replaceFirst("\\{\\}", String.valueOf(value));
        }
            
        System.out.println("[Debug/" + category.getId() + "] " + fullMessage);
    }
    
    /**
     * Clears the log value cache, optionally for a specific category only.
     * 
     * @param category The specific category to clear cache for, or null to clear all categories
     */
    public static void clearCache(Category category) {
        if (category == null) {
            LAST_LOG_VALUES.clear();
            return;
        }
        
        LAST_LOG_VALUES.keySet().removeIf(key -> key.startsWith(category.getId() + "::"));
    }
    
    /**
     * Clears the entire log value cache for all categories.
     */
    public static void clearCache() {
        clearCache(null);
    }
}