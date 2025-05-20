package com.enoughfolders.data;

import com.enoughfolders.EnoughFolders;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


/**
 * Handles saving and loading folder data for Minecraft worlds.
 */
public class WorldStorage {
    /** Directory name for Minecraft config folder */
    private static final String CONFIG_DIR = "config";
    /** Directory name for this mod's config folder */
    private static final String MOD_DIR = "enoughfolders";
    /** Directory name for world-specific configurations */
    private static final String WORLDS_DIR = "worlds";
    /** Filename for the folders configuration file */
    private static final String FOLDERS_FILE = "folders.json";
    /** Gson instance for JSON serialization/deserialization */
    private final Gson gson;
    
    /**
     * Creates a new WorldStorage instance.
     */
    public WorldStorage() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        ensureDirectoriesExist();
    }
    
    /**
     * Gets the name of the current Minecraft world.
     *
     * @return The current world name, or null if no world is loaded
     */
    public String getWorldName() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return null;
        }
        
        // For multiplayer servers
        if (!minecraft.isLocalServer()) {
            // Safe handling of server info
            var currentServer = minecraft.getCurrentServer();
            String serverName = (currentServer != null && currentServer.name != null) 
                ? currentServer.name 
                : "unknown_server";
            return "mp_" + serverName.replaceAll("[^a-zA-Z0-9_-]", "_");
        }
        
        // For single player worlds
        try {
            var singlePlayerServer = minecraft.getSingleplayerServer();
            if (singlePlayerServer != null) {
                var worldData = singlePlayerServer.getWorldData();
                if (worldData != null) {
                    String worldName = worldData.getLevelName();
                    if (worldName != null && !worldName.isEmpty()) {
                        return worldName;
                    }
                }
            }
            return "unknown";
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to get world name", e);
            return "unknown";
        }
    }
    
    /**
     * Loads folders configuration for the specified world.
     *
     * @param worldName The name of the world to load folders for
     * @return A list of folders configured for the specified world
     */
    public List<Folder> loadFolders(String worldName) {
        File foldersFile = getFoldersFile(worldName);
        if (!foldersFile.exists()) {
            return new ArrayList<>();
        }
        
        try (FileReader reader = new FileReader(foldersFile)) {
            Type folderListType = new TypeToken<ArrayList<Folder>>(){}.getType();
            return gson.fromJson(reader, folderListType);
        } catch (IOException e) {
            EnoughFolders.LOGGER.error("Failed to load folders for world: " + worldName, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Saves the folders configuration for the specified world.
     *
     * @param worldName The name of the world to save folders for
     * @param folders The list of folders to save
     */
    public void saveFolders(String worldName, List<Folder> folders) {
        File foldersFile = getFoldersFile(worldName);
        try (FileWriter writer = new FileWriter(foldersFile)) {
            gson.toJson(folders, writer);
        } catch (IOException e) {
            EnoughFolders.LOGGER.error("Failed to save folders for world: " + worldName, e);
        }
    }
    
    /**
     * Gets the file that stores folder configurations for the specified world.
     *
     * @param worldName The name of the world
     * @return The folders.json file for the specified world
     */
    private File getFoldersFile(String worldName) {
        File worldDir = getWorldDirectory(worldName);
        return new File(worldDir, FOLDERS_FILE);
    }
    
    /**
     * Gets the directory for a specific world's configurations.
     *
     * @param worldName The name of the world
     * @return The world-specific directory
     */
    private File getWorldDirectory(String worldName) {
        File modWorldsDir = getModWorldsDirectory();
        File worldDir = new File(modWorldsDir, worldName);
        if (!worldDir.exists() && !worldDir.mkdirs()) {
            EnoughFolders.LOGGER.error("Failed to create world directory: " + worldDir.getPath());
        }
        return worldDir;
    }
    
    /**
     * Gets the directory that contains all world-specific configuration directories.
     *
     * @return The worlds directory
     */
    private File getModWorldsDirectory() {
        File modDir = getModDirectory();
        File worldsDir = new File(modDir, WORLDS_DIR);
        if (!worldsDir.exists() && !worldsDir.mkdirs()) {
            EnoughFolders.LOGGER.error("Failed to create worlds directory: " + worldsDir.getPath());
        }
        return worldsDir;
    }
    
    /**
     * Gets the mod's configuration directory.
     *
     * @return The mod's configuration directory
     */
    private File getModDirectory() {
        File configDir = getConfigDirectory();
        File modDir = new File(configDir, MOD_DIR);
        if (!modDir.exists() && !modDir.mkdirs()) {
            EnoughFolders.LOGGER.error("Failed to create mod directory: " + modDir.getPath());
        }
        return modDir;
    }
    
    /**
     * Gets Minecraft's configuration directory.
     *
     * @return Minecraft's configuration directory
     */
    private File getConfigDirectory() {
        File gameDir = Minecraft.getInstance().gameDirectory;
        File configDir = new File(gameDir, CONFIG_DIR);
        if (!configDir.exists() && !configDir.mkdirs()) {
            EnoughFolders.LOGGER.error("Failed to create config directory: " + configDir.getPath());
        }
        return configDir;
    }
    
    /**
     * Ensures that all required directories for this mod exist.
     */
    private void ensureDirectoriesExist() {
        // Just make sure the base directories exist
        getModWorldsDirectory();
    }
}
