package com.enoughfolders.integrations.api;

import com.enoughfolders.data.Folder;
import com.enoughfolders.data.StoredIngredient;

import java.util.Optional;

/**
 * Interface for drag-and-drop operations
 */
public interface IngredientDragProvider {
    
    /**
     * Gets the currently dragged ingredient, if any.
     *
     * @return Optional containing the dragged ingredient, or empty if none is being dragged
     */
    Optional<?> getDraggedIngredient();
    
    /**
     * Checks if an ingredient is currently being dragged.
     * 
     * @return True if an ingredient is being dragged, false otherwise
     */
    default boolean isIngredientBeingDragged() {
        return getDraggedIngredient().isPresent();
    }
    
    /**
     * Processes a drop of the currently dragged ingredient onto a folder.
     * 
     * @param folder The folder to add the ingredient to
     * @return True if the drop was successful, false otherwise
     */
    boolean handleIngredientDrop(Folder folder);
    
    /**
     * Gets the display name of the integration.
     * 
     * @return The display name
     */
    String getDisplayName();
    
    /**
     * Checks if the integration is available.
     * 
     * @return True if the integration is available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Converts a StoredIngredient back to its original integration-specific object.
     *
     * @param storedIngredient The stored ingredient to convert
     * @return Optional containing the original ingredient object, or empty if conversion failed
     */
    Optional<?> getIngredientFromStored(StoredIngredient storedIngredient);
    
    /**
     * Converts an integration-specific ingredient object to a StoredIngredient.
     *
     * @param ingredient The integration-specific ingredient object to convert
     * @return Optional containing the StoredIngredient, or empty if conversion failed
     */
    Optional<StoredIngredient> storeIngredient(Object ingredient);
}
