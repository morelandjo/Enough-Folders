package com.enoughfolders.data;

import com.enoughfolders.util.DebugLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a folder
 */
public class Folder {
    /**
     * Unique identifier for this folder, used for serialization and equality checks.
     */
    private final String id;
    
    /**
     * Display name of the folder, shown in the UI.
     */
    private String name;
    
    /**
     * List of ingredients stored in this folder.
     */
    private List<StoredIngredient> ingredients;
    
    /**
     * Whether this folder is the currently active folder.
     */
    private boolean active;
    
    /**
     * Creates a new folder with the specified name.
     *
     * @param name The display name for the folder
     */
    public Folder(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.ingredients = new ArrayList<>();
        this.active = false;
        DebugLogger.debug(DebugLogger.Category.FOLDER_MANAGER, "Created new folder: " + name + " with ID: " + id);
    }
    
    /**
     * Creates a folder with specified properties.
     *
     * @param id The unique identifier for the folder
     * @param name The display name for the folder
     * @param ingredients The list of ingredients in the folder
     * @param active Whether the folder is active
     */
    public Folder(String id, String name, List<StoredIngredient> ingredients, boolean active) {
        this.id = id;
        this.name = name;
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
        this.active = active;
        DebugLogger.debug(DebugLogger.Category.FOLDER_MANAGER, "Loaded existing folder: " + name + " with ID: " + id);
    }
    
    /**
     * Gets the unique identifier for this folder.
     *
     * @return The folder's UUID as a string
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the display name of this folder.
     *
     * @return The folder's name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Changes the display name of this folder.
     *
     * @param name The new name for the folder
     */
    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        DebugLogger.debug(DebugLogger.Category.FOLDER_MANAGER, "Renamed folder from '" + oldName + "' to '" + name + "'");
    }
    
    /**
     * Gets all ingredients stored in this folder.
     *
     * @return The list of stored ingredients
     */
    public List<StoredIngredient> getIngredients() {
        return ingredients;
    }
    
    /**
     * Adds an ingredient to this folder if it's not already present.
     *
     * @param ingredient The ingredient to add to the folder
     */
    public void addIngredient(StoredIngredient ingredient) {
        if (!ingredients.contains(ingredient)) {
            ingredients.add(ingredient);
            DebugLogger.debugValues(DebugLogger.Category.FOLDER_MANAGER, 
                "Added ingredient {}/{} to folder '{}'", ingredient.getType(), ingredient.getValue(), name);
        }
    }
    
    /**
     * Removes an ingredient from this folder.
     *
     * @param ingredient The ingredient to remove from the folder
     */
    public void removeIngredient(StoredIngredient ingredient) {
        ingredients.remove(ingredient);
        DebugLogger.debugValues(DebugLogger.Category.FOLDER_MANAGER, 
            "Removed ingredient {}/{} from folder '{}'", ingredient.getType(), ingredient.getValue(), name);
    }
    
    /**
     * Checks if this folder is currently active.
     *
     * @return true if this folder is active, false otherwise
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Sets whether this folder is active or inactive.
     *
     * @param active true to make this folder active, false to make it inactive
     */
    public void setActive(boolean active) {
        this.active = active;
        DebugLogger.debugValues(DebugLogger.Category.FOLDER_MANAGER, 
            "Folder '{}' active state changed to: {}", name, active);
    }
    
    /**
     * Gets a shortened version of the folder name.
     *
     * @return The short name for this folder
     */
    public String getShortName() {
        if (name.length() <= 3) {
            return name;
        }
        return name.substring(0, 3);
    }
    
    /**
     * Gets a truncated version of the folder name for medium-sized UI elements.
     *
     * @return The truncated name for this folder
     */
    public String getTruncatedName() {
        if (name.length() <= 12) {
            return name;
        }
        String truncated = name.substring(0, 13) + "...";
        DebugLogger.debugValues(DebugLogger.Category.FOLDER_MANAGER, 
            "Truncated folder name '{}' to '{}'", name, truncated);
        return truncated;
    }
    
    /**
     * Checks if this folder is equal to another object.
     *
     * @param obj The object to compare with
     * @return true if the folders are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Folder folder = (Folder) obj;
        return id.equals(folder.id);
    }
    
    /**
     * Generates a hash code for this folder based on its ID.
     *
     * @return The hash code value
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
