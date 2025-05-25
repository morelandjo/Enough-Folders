package com.enoughfolders.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for FolderManager
 */
public class FolderManagerTest {

    private FolderManagerForTest folderManager;
    
    @BeforeEach
    public void setup() {
        // Create a new instance before each test
        folderManager = new FolderManagerForTest();
    }
    
    @Test
    public void testCreateFolder() {
        String folderName = "Test Folder";
        Folder folder = folderManager.createFolder(folderName);
        
        assertNotNull(folder, "Folder should not be null");
        assertEquals(folderName, folder.getName(), "Folder name should match");
        
        // Verify the folder was added to the manager's list
        assertTrue(folderManager.getFolders().contains(folder), 
            "Created folder should be in the folder list");
    }
    
    @Test
    public void testGetActiveFolder() {
        // Initially there should be no active folder
        assertTrue(folderManager.getActiveFolder().isEmpty(), 
            "Initially there should be no active folder");
            
        // Create and set active folder
        Folder folder = folderManager.createFolder("Active Folder");
        folderManager.setActiveFolder(folder);
        
        // Verify active folder is set correctly
        Optional<Folder> activeFolder = folderManager.getActiveFolder();
        assertTrue(activeFolder.isPresent(), "Active folder should be present");
        assertEquals(folder, activeFolder.get(), "Active folder should match the set folder");
    }
    
    @Test
    public void testDeleteFolder() {
        // Create a folder
        Folder folder = folderManager.createFolder("To Delete");
        
        // Verify it exists
        assertTrue(folderManager.getFolders().contains(folder), 
            "Folder should exist before deletion");
            
        // Delete the folder
        folderManager.deleteFolder(folder);
        
        // Verify it was deleted
        assertFalse(folderManager.getFolders().contains(folder), 
            "Folder should not exist after deletion");
    }
    
    @Test
    public void testAddIngredient() {
        // Create a folder and an ingredient
        Folder folder = folderManager.createFolder("Ingredients");
        StoredIngredient ingredient = Mockito.mock(StoredIngredient.class);
        
        // Add the ingredient
        folderManager.addIngredient(folder, ingredient);
        
        // Verify the ingredient was added
        assertTrue(folder.getIngredients().contains(ingredient), 
            "Ingredient should be in the folder's list");
    }
    
    @Test
    public void testRemoveIngredient() {
        // Create a folder and an ingredient
        Folder folder = folderManager.createFolder("Ingredients");
        StoredIngredient ingredient = Mockito.mock(StoredIngredient.class);
        
        // Add and then remove the ingredient
        folderManager.addIngredient(folder, ingredient);
        folderManager.removeIngredient(folder, ingredient);
        
        // Verify the ingredient was removed
        assertFalse(folder.getIngredients().contains(ingredient), 
            "Ingredient should not be in the folder's list after removal");
    }
}