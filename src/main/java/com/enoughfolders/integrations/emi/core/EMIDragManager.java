package com.enoughfolders.integrations.emi.core;

import com.enoughfolders.data.Folder;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.util.DebugLogger;

import java.util.Optional;

/**
 * Manages drag and drop operations for EMI integration.
 */
public class EMIDragManager {
    
    private static boolean initialized = false;
    
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            DebugLogger.debug(
                DebugLogger.Category.INTEGRATION,
                "Initializing EMI drag manager"
            );
            
            initialized = true;
            
            DebugLogger.debug(
                DebugLogger.Category.INTEGRATION,
                "EMI drag manager initialized"
            );
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error initializing EMI drag manager: {}", 
                e.getMessage()
            );
        }
    }
    
    /**
     * Get the currently dragged ingredient from EMI.
     */
    public static Optional<?> getDraggedIngredient() {
        if (!initialized) {
            return Optional.empty();
        }
        
        try {
            // EMI doesn't have a direct drag state API like JEI/REI
            // Instead, we can check if there's a hovered ingredient that might be dragged
            Object hoveredIngredient = EMIRecipeManager.getHoveredIngredient();
            if (hoveredIngredient != null) {
                return Optional.of(hoveredIngredient);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error getting dragged EMI ingredient: {}", e.getMessage()
            );
            return Optional.empty();
        }
    }
    
    /**
     * Handle dropping an ingredient onto a folder.
     */
    public static boolean handleIngredientDrop(Folder folder) {
        if (!initialized || folder == null) {
            return false;
        }
        
        try {
            Optional<?> draggedOpt = getDraggedIngredient();
            if (draggedOpt.isEmpty()) {
                return false;
            }
            
            Object draggedIngredient = draggedOpt.get();
            
            // Convert EMI ingredient to StoredIngredient
            Optional<StoredIngredient> storedOpt = EMIIngredientManager.storeIngredient(draggedIngredient);
            if (storedOpt.isEmpty()) {
                return false;
            }
            
            StoredIngredient storedIngredient = storedOpt.get();
            
            // Add to folder
            folder.addIngredient(storedIngredient);
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Successfully dropped EMI ingredient into folder: {}", storedIngredient.getValue()
            );
            
            return true;
            
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error handling EMI ingredient drop: {}", e.getMessage()
            );
            return false;
        }
    }
    
    /**
     * Check if an ingredient is currently being dragged.
     */
    public static boolean isIngredientBeingDragged() {
        return getDraggedIngredient().isPresent();
    }
    
    /**
     * Check if EMI drag operations are supported.
     */
    public static boolean isDragSupported() {
        return initialized;
    }
}
