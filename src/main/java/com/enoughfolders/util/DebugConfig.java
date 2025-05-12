package com.enoughfolders.util;

import com.enoughfolders.EnoughFolders;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration manager for debug settings.
 */
public class DebugConfig {
    /** 
     * Name of the debug settings configuration file
     */
    private static final String CONFIG_FILE = "debug_settings.json";
    
    /**
     * Gson instance for JSON serialization/deserialization
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Default enabled/disabled states for each debug category
     */
    private static final Map<DebugLogger.Category, Boolean> DEFAULT_SETTINGS = new HashMap<>();
    
    static {
        // Initialize default settings
        DEFAULT_SETTINGS.put(DebugLogger.Category.INITIALIZATION, true);
        DEFAULT_SETTINGS.put(DebugLogger.Category.RENDERING, false);
        DEFAULT_SETTINGS.put(DebugLogger.Category.INPUT, true);
        DEFAULT_SETTINGS.put(DebugLogger.Category.FOLDER_MANAGER, true);
        DEFAULT_SETTINGS.put(DebugLogger.Category.INTEGRATION, true);
        DEFAULT_SETTINGS.put(DebugLogger.Category.GUI_STATE, true);
        DEFAULT_SETTINGS.put(DebugLogger.Category.MOUSE, false);
        DEFAULT_SETTINGS.put(DebugLogger.Category.JEI_INTEGRATION, true);
    }
    
    /**
     * Loads debug settings from the config file.
     */
    public static void load() {
        Path configDir = getConfigDirectory();
        Path configFile = configDir.resolve(CONFIG_FILE);
        
        // Create config directory if it doesn't exist
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                EnoughFolders.LOGGER.error("Failed to create config directory", e);
                applyDefaultSettings();
                return;
            }
        }
        
        // Check if config file exists
        if (!Files.exists(configFile)) {
            // Create default config file
            save();
            applyDefaultSettings();
            return;
        }
        
        // Load settings from config file
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile.toFile()))) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            
            if (json == null) {
                EnoughFolders.LOGGER.warn("Debug config file is empty or malformed, using defaults");
                applyDefaultSettings();
                return;
            }
            
            // Apply settings from JSON
            for (DebugLogger.Category category : DebugLogger.Category.values()) {
                String categoryId = category.getId();
                boolean enabled = DEFAULT_SETTINGS.get(category);
                
                if (json.has(categoryId)) {
                    enabled = json.get(categoryId).getAsBoolean();
                }
                
                DebugLogger.setEnabled(category, enabled);
            }
            
            EnoughFolders.LOGGER.info("Loaded debug settings from " + configFile);
            
        } catch (JsonSyntaxException e) {
            EnoughFolders.LOGGER.error("Error parsing debug config file", e);
            applyDefaultSettings();
        } catch (IOException e) {
            EnoughFolders.LOGGER.error("Failed to read debug config file", e);
            applyDefaultSettings();
        }
    }
    
    /**
     * Saves current debug settings to the config file.
     */
    public static void save() {
        Path configDir = getConfigDirectory();
        Path configFile = configDir.resolve(CONFIG_FILE);
        
        // Create config directory if it doesn't exist
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                EnoughFolders.LOGGER.error("Failed to create config directory", e);
                return;
            }
        }
        
        // Create JSON object with current settings
        JsonObject json = new JsonObject();
        for (DebugLogger.Category category : DebugLogger.Category.values()) {
            json.addProperty(category.getId(), DebugLogger.isEnabled(category));
        }
        
        // Write JSON to config file
        try (FileWriter writer = new FileWriter(configFile.toFile())) {
            GSON.toJson(json, writer);
            EnoughFolders.LOGGER.info("Saved debug settings to " + configFile);
        } catch (IOException e) {
            EnoughFolders.LOGGER.error("Failed to save debug config file", e);
        }
    }
    
    /**
     * Applies default settings to all debug categories.
     */
    private static void applyDefaultSettings() {
        for (DebugLogger.Category category : DebugLogger.Category.values()) {
            boolean defaultEnabled = DEFAULT_SETTINGS.getOrDefault(category, true);
            DebugLogger.setEnabled(category, defaultEnabled);
        }
        EnoughFolders.LOGGER.info("Applied default debug settings");
    }
    
    /**
     * Gets the config directory path for this mod.
     * 
     * @return The config directory path for the mod
     */
    private static Path getConfigDirectory() {
        return Paths.get("config", EnoughFolders.MOD_ID);
    }
}