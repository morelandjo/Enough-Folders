package com.enoughfolders.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Objects;

/**
 * Unit tests for the StoredIngredient class
 */
public class StoredIngredientTest {

    @Test
    public void testConstructorAndGetters() {
        String type = "minecraft:stone";
        String value = "item";
        
        StoredIngredient ingredient = new StoredIngredient(type, value);
        
        assertEquals(type, ingredient.getType(), "Type should match constructor argument");
        assertEquals(value, ingredient.getValue(), "Value should match constructor argument");
    }
    
    @Test
    public void testEquals() {
        StoredIngredient ingredient1 = new StoredIngredient("minecraft:stone", "item");
        StoredIngredient ingredient2 = new StoredIngredient("minecraft:stone", "item");
        StoredIngredient ingredient3 = new StoredIngredient("minecraft:dirt", "item");
        StoredIngredient ingredient4 = new StoredIngredient("minecraft:stone", "block");
        
        assertTrue(ingredient1.equals(ingredient1), "Ingredient should equal itself");
        assertTrue(ingredient1.equals(ingredient2), "Ingredients with same type and value should be equal");
        assertFalse(ingredient1.equals(ingredient3), "Ingredients with different types should not be equal");
        assertFalse(ingredient1.equals(ingredient4), "Ingredients with different values should not be equal");
        assertFalse(ingredient1.equals(null), "Ingredient should not equal null");
        assertFalse(ingredient1.equals("not an ingredient"), "Ingredient should not equal different type");
    }
    
    @Test
    public void testHashCode() {
        String type = "minecraft:stone";
        String value = "item";
        StoredIngredient ingredient = new StoredIngredient(type, value);
        
        assertEquals(Objects.hash(type, value), ingredient.hashCode(), 
            "HashCode should match hash of type and value");
        
        // Test consistency
        assertEquals(ingredient.hashCode(), ingredient.hashCode(), 
            "HashCode should be consistent across calls");
        
        // Test different ingredients have different hashcodes
        StoredIngredient differentIngredient = new StoredIngredient("minecraft:dirt", "item");
        assertNotEquals(ingredient.hashCode(), differentIngredient.hashCode(), 
            "Different ingredients should have different hashcodes");
    }
    
    @Test
    public void testToString() {
        StoredIngredient ingredient = new StoredIngredient("minecraft:stone", "item");
        String expected = "StoredIngredient{type='minecraft:stone', value='item'}";
        
        assertEquals(expected, ingredient.toString(), "toString output should match expected format");
    }
}