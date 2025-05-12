package com.enoughfolders.data;

import java.util.Objects;

/**
 * Represents an ingredient stored in a folder.
 */
public class StoredIngredient {
    /**
     * The type of ingredient
     */
    private final String type;
    
    /**
     * The unique identifier for the ingredient.
     */
    private final String value;
    
    /**
     * Creates a new stored ingredient.
     *
     * @param type The type identifier of the ingredient (usually a class name)
     * @param value The unique value or identifier for the specific ingredient
     */
    public StoredIngredient(String type, String value) {
        this.type = type;
        this.value = value;
    }
    
    /**
     * Gets the type identifier for this ingredient.
     *
     * @return The type string, typically a fully qualified class name
     */
    public String getType() {
        return type;
    }
    
    /**
     * Gets the value identifier for this ingredient.
     *
     * @return The value string that uniquely identifies this ingredient within its type
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Checks if this StoredIngredient is equal to another object.
     *
     * @param o The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredIngredient that = (StoredIngredient) o;
        return Objects.equals(type, that.type) && 
               Objects.equals(value, that.value);
    }
    
    /**
     * Generates a hash code for this ingredient.
     *
     * @return A hash code value based on the type and value fields
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }
    
    /**
     * Returns a string representation of this StoredIngredient.
     *
     * @return A string representation showing the type and value
     */
    @Override
    public String toString() {
        return "StoredIngredient{" +
               "type='" + type + '\'' +
               ", value='" + value + '\'' +
               '}';
    }
}
