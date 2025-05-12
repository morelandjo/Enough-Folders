package com.enoughfolders.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the Folder class
 */
public class FolderTest {

    @Test
    public void testFolderConstructor() {
        String name = "Test Folder";
        Folder folder = new Folder(name);
        
        assertNotNull(folder.getId(), "Folder ID should not be null");
        assertEquals(name, folder.getName(), "Folder name should match");
        assertEquals(0, folder.getIngredients().size(), "New folder should have no ingredients");
        assertFalse(folder.isActive(), "New folder should not be active by default");
    }
    
    @Test
    public void testFolderFullConstructor() {
        String id = "test-id-123";
        String name = "Test Folder";
        List<StoredIngredient> ingredients = new ArrayList<>();
        ingredients.add(new StoredIngredient("minecraft:stone", "item"));
        boolean active = true;
        
        Folder folder = new Folder(id, name, ingredients, active);
        
        assertEquals(id, folder.getId(), "Folder ID should match");
        assertEquals(name, folder.getName(), "Folder name should match");
        assertEquals(1, folder.getIngredients().size(), "Folder should have one ingredient");
        assertTrue(folder.isActive(), "Folder should be active");
    }
    
    @Test
    public void testFolderNullIngredientsList() {
        String id = "test-id-123";
        String name = "Test Folder";
        
        Folder folder = new Folder(id, name, null, false);
        
        assertNotNull(folder.getIngredients(), "Ingredients list should not be null even when constructed with null");
        assertEquals(0, folder.getIngredients().size(), "Ingredients list should be empty");
    }
    
    @Test
    public void testAddIngredient() {
        Folder folder = new Folder("Test Folder");
        StoredIngredient ingredient = new StoredIngredient("minecraft:stone", "item");
        
        folder.addIngredient(ingredient);
        
        assertEquals(1, folder.getIngredients().size(), "Folder should have one ingredient");
        assertEquals(ingredient, folder.getIngredients().get(0), "Ingredient should match");
    }
    
    @Test
    public void testAddDuplicateIngredient() {
        Folder folder = new Folder("Test Folder");
        StoredIngredient ingredient = new StoredIngredient("minecraft:stone", "item");
        
        folder.addIngredient(ingredient);
        folder.addIngredient(ingredient);
        
        assertEquals(1, folder.getIngredients().size(), "Folder should not add duplicate ingredients");
    }
    
    @Test
    public void testRemoveIngredient() {
        Folder folder = new Folder("Test Folder");
        StoredIngredient ingredient = new StoredIngredient("minecraft:stone", "item");
        
        folder.addIngredient(ingredient);
        assertEquals(1, folder.getIngredients().size(), "Folder should have one ingredient");
        
        folder.removeIngredient(ingredient);
        assertEquals(0, folder.getIngredients().size(), "Folder should have no ingredients after removal");
    }
    
    @Test
    public void testSetActive() {
        Folder folder = new Folder("Test Folder");
        assertFalse(folder.isActive(), "New folder should not be active by default");
        
        folder.setActive(true);
        assertTrue(folder.isActive(), "Folder should be active after setting to active");
        
        folder.setActive(false);
        assertFalse(folder.isActive(), "Folder should not be active after setting to inactive");
    }
    
    @Test
    public void testSetName() {
        Folder folder = new Folder("Old Name");
        assertEquals("Old Name", folder.getName(), "Initial name should match");
        
        folder.setName("New Name");
        assertEquals("New Name", folder.getName(), "Name should be updated after setName");
    }
    
    @Test
    public void testGetShortName() {
        Folder shortFolder = new Folder("ABC");
        assertEquals("ABC", shortFolder.getShortName(), "Short name should be the same for short folder names");
        
        Folder longFolder = new Folder("Long Folder Name");
        assertEquals("Lon", longFolder.getShortName(), "Short name should be first three chars for long folder names");
    }
    
    @Test
    public void testGetTruncatedName() {
        Folder shortFolder = new Folder("Short Name");
        assertEquals("Short Name", shortFolder.getTruncatedName(), "Truncated name should be the same for short folder names");
        
        Folder longFolder = new Folder("Very Long Folder Name That Should Be Truncated");
        assertEquals("Very Long Fol...", longFolder.getTruncatedName(), "Long names should be truncated with ellipsis");
    }
    
    @Test
    public void testEquals() {
        String id = "same-id-123";
        Folder folder1 = new Folder(id, "Folder 1", null, false);
        Folder folder2 = new Folder(id, "Folder 2", null, true);
        Folder folder3 = new Folder("different-id", "Folder 1", null, false);
        
        assertTrue(folder1.equals(folder1), "Folder should equal itself");
        assertTrue(folder1.equals(folder2), "Folders with same ID should be equal even with different names/state");
        assertFalse(folder1.equals(folder3), "Folders with different IDs should not be equal");
        assertFalse(folder1.equals(null), "Folder should not equal null");
        assertFalse(folder1.equals("not a folder"), "Folder should not equal different type");
    }
    
    @Test
    public void testHashCode() {
        String id = "test-id-123";
        Folder folder = new Folder(id, "Test Folder", null, false);
        
        assertEquals(id.hashCode(), folder.hashCode(), "Folder hashCode should match its ID hashCode");
    }
}