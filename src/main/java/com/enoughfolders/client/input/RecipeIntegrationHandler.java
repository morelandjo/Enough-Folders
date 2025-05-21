package com.enoughfolders.client.input;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.util.DebugLogger;

/**
 * A central handler for recipe mod integration keyboard events.
 */
public class RecipeIntegrationHandler {
    /**
     * Flag to track if JEI is available
     */
    private static boolean jeiAvailable = false;
    
    /**
     * Flag to track if REI is available
     */
    private static boolean reiAvailable = false;
    
    /**
     * Initializes the handler and checks which integrations are available.
     */
    public static void init() {
        // Check if JEI and REI are available without directly importing their classes
        try {
            Class.forName("mezz.jei.api.runtime.IJeiRuntime");
            jeiAvailable = true;
            EnoughFolders.LOGGER.info("JEI runtime classes found");
        } catch (ClassNotFoundException e) {
            jeiAvailable = false;
            EnoughFolders.LOGGER.info("JEI runtime classes not found");
        }
        
        try {
            Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            reiAvailable = true;
            EnoughFolders.LOGGER.info("REI runtime classes found");
        } catch (ClassNotFoundException e) {
            reiAvailable = false;
            EnoughFolders.LOGGER.info("REI runtime classes not found");
        }
        
        EnoughFolders.LOGGER.info("Recipe integration handler initialized, JEI: {}, REI: {}", jeiAvailable, reiAvailable);
    }
    
    /**
     * Handles the "Add to Folder" key press.
     */
    public static void handleAddToFolderKeyPress() {
        EnoughFolders.LOGGER.debug("Add to folder key press detected");
        
        // Try both integrations, they will safely no-op if not applicable
        if (jeiAvailable) {
            tryJeiAddToFolder();
        }
        
        if (reiAvailable) {
            tryReiAddToFolder();
        }
    }
    
    /**
     * Attempts to add an ingredient to a folder using JEI integration.
     */
    private static void tryJeiAddToFolder() {
        try {
            com.enoughfolders.integrations.jei.handlers.JEIAddToFolderHandler.handleAddToFolderKeyPress();
            DebugLogger.debug(DebugLogger.Category.INPUT, "JEI add to folder handler executed");
        } catch (Throwable t) {
            // If it fails for any reason, log it but don't crash
            EnoughFolders.LOGGER.error("Failed to handle JEI add to folder", t);
        }
    }
    
    /**
     * Attempts to add an ingredient to a folder using REI integration.
     */
    private static void tryReiAddToFolder() {
        try {
            com.enoughfolders.integrations.rei.handlers.REIAddToFolderHandler.handleAddToFolderKeyPress();
            DebugLogger.debug(DebugLogger.Category.INPUT, "REI add to folder handler executed");
        } catch (Throwable t) {
            // If it fails for any reason, log it but don't crash
            EnoughFolders.LOGGER.error("Failed to handle REI add to folder", t);
        }
    }
}
