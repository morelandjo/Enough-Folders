package com.enoughfolders.data;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.util.DebugLogger;
import com.enoughfolders.util.DebugLogger.Category;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles saving and loading folder data to/from disk.
 */
public class StorageManager {
    /**
     * Name of the mod's config directory
     */
    private static final String CONFIG_DIR = "enoughfolders";
    
    /**
     * Directory name for world-specific folder data
     */
    private static final String WORLDS_DIR = "worlds";
    
    /**
     * Filename for folder data in each world directory
     */
    private static final String FOLDERS_FILE = "folders.json";
    
    /**
     * Gson instance for JSON serialization/deserialization
     */
    private final Gson gson;
    
    /**
     * Creates a new storage manager with a configured Gson instance.
     */
    public StorageManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    /**
     * Loads folders for a specific world from disk.
     * 
     * @param worldName The name or identifier for the world
     * @return List of folder objects loaded from disk
     * @throws IOException If there's an error reading the folder data file
     */
    public List<Folder> loadFolders(String worldName) throws IOException {
        File file = getFoldersFile(worldName);
        
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<Folder>>(){}.getType();
            return gson.fromJson(reader, listType);
        }
    }
    
    /**
     * Saves folders for a specific world to disk.
     * 
     * @param worldName The name or identifier for the world
     * @param folders The list of folders to save
     * @throws IOException If there's an error writing the folder data file
     */
    public void saveFolders(String worldName, List<Folder> folders) throws IOException {
        File file = getFoldersFile(worldName);
        
        // Create parent directories if they don't exist
        file.getParentFile().mkdirs();
        
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(folders, writer);
        }
    }
    
    /**
     * Gets the file reference for storing folder data for a specific world.
     * 
     * @param worldName The name or identifier for the world
     * @return File object pointing to the world's folder data file
     */
    private File getFoldersFile(String worldName) {
        Path configPath = FMLPaths.CONFIGDIR.get();
        Path worldFolderPath = configPath.resolve(CONFIG_DIR).resolve(WORLDS_DIR).resolve(worldName);
        
        try {
            Files.createDirectories(worldFolderPath);
        } catch (IOException e) {
            EnoughFolders.LOGGER.error("Failed to create directory for world data", e);
        }
        
        return worldFolderPath.resolve(FOLDERS_FILE).toFile();
    }
}
