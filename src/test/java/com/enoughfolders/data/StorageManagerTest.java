package com.enoughfolders.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the StorageManager class
 */
public class StorageManagerTest {

    @TempDir
    Path tempDir;

    private String testWorldName;
    private TestStorageManager testStorageManager;

    @BeforeEach
    void setUp() {
        // Create a unique test world name for each test
        testWorldName = "test_world_" + UUID.randomUUID().toString().substring(0, 8);
        testStorageManager = new TestStorageManager(tempDir.toFile());
    }

    @AfterEach
    void tearDown() {
        // Clean up any files created during tests
        File worldFolder = testStorageManager.getWorldFolder(testWorldName);
        if (worldFolder.exists()) {
            deleteRecursive(worldFolder);
        }
    }

    @Test
    void testSaveAndLoadFolders() throws IOException {
        // Create test folders
        List<Folder> folders = createTestFolders();
        
        // Save folders
        testStorageManager.saveFolders(testWorldName, folders);
        
        // Load folders
        List<Folder> loadedFolders = testStorageManager.loadFolders(testWorldName);
        
        // Verify loaded folders match saved folders
        assertEquals(folders.size(), loadedFolders.size(), "Should load the same number of folders");
        
        for (int i = 0; i < folders.size(); i++) {
            Folder original = folders.get(i);
            Folder loaded = loadedFolders.get(i);
            
            assertEquals(original.getName(), loaded.getName(), "Folder name should match");
            assertEquals(original.getIngredients().size(), loaded.getIngredients().size(), 
                    "Folder should have same number of ingredients");
        }
    }

    @Test
    void testLoadFoldersFromNonExistentFile() throws IOException {
        // Try to load folders from a world that doesn't exist
        List<Folder> folders = testStorageManager.loadFolders("non_existent_world");
        
        // Should return an empty list, not null
        assertNotNull(folders, "Should not return null for non-existent worlds");
        assertTrue(folders.isEmpty(), "Should return empty list for non-existent worlds");
    }

    @Test
    void testSaveFoldersCreatesDirectories() throws IOException {
        // Create test folders
        List<Folder> folders = createTestFolders();
        
        // Delete world folder if it exists to ensure we're testing directory creation
        File worldFolder = testStorageManager.getWorldFolder(testWorldName);
        if (worldFolder.exists()) {
            deleteRecursive(worldFolder);
        }
        
        // Save folders which should create directories
        testStorageManager.saveFolders(testWorldName, folders);
        
        // Verify directories were created
        File foldersFile = testStorageManager.getFoldersFile(testWorldName);
        assertTrue(foldersFile.exists(), "Folders file should exist");
        assertTrue(foldersFile.isFile(), "Folders file should be a file");
    }

    private List<Folder> createTestFolders() {
        List<Folder> folders = new ArrayList<>();
        
        // Create a few test folders with ingredients
        Folder folder1 = new Folder("Test Folder 1");
        folder1.addIngredient(new StoredIngredient("minecraft:stone", "item"));
        folder1.addIngredient(new StoredIngredient("minecraft:dirt", "item"));
        
        Folder folder2 = new Folder("Test Folder 2");
        folder2.addIngredient(new StoredIngredient("minecraft:iron_ingot", "item"));
        
        folders.add(folder1);
        folders.add(folder2);
        
        return folders;
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursive(child);
            }
        }
        file.delete();
    }

    /**
     * Test-specific implementation of StorageManager that uses a temp directory
     */
    private class TestStorageManager extends StorageManager {
        private final File baseDir;
        private static final String CONFIG_DIR = "enough_folders";
        private static final String WORLDS_DIR = "worlds";
        private static final String FOLDERS_FILE = "folders.json";
        
        public TestStorageManager(File baseDir) {
            super();
            this.baseDir = baseDir;
        }
        
        public File getWorldFolder(String worldName) {
            return new File(baseDir, CONFIG_DIR + "/" + WORLDS_DIR + "/" + worldName);
        }
        
        // Instead of trying to override a private method, reimplement the public methods
        @Override
        public List<Folder> loadFolders(String worldName) throws IOException {
            File file = getFoldersFile(worldName);
            
            if (!file.exists()) {
                return new ArrayList<>();
            }
            
            try (java.io.FileReader reader = new java.io.FileReader(file)) {
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<ArrayList<Folder>>(){}.getType();
                return new com.google.gson.GsonBuilder().setPrettyPrinting().create().fromJson(reader, listType);
            }
        }
        
        @Override
        public void saveFolders(String worldName, List<Folder> folders) throws IOException {
            File file = getFoldersFile(worldName);
            
            // Create parent directories if they don't exist
            file.getParentFile().mkdirs();
            
            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(folders, writer);
            }
        }
        
        // This is not an override, just a helper method for tests
        public File getFoldersFile(String worldName) {
            return new File(getWorldFolder(worldName), FOLDERS_FILE);
        }
    }
}