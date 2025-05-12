package com.enoughfolders.data;

import com.enoughfolders.EnoughFolders;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages all folders and their contents.
 */
public class FolderManager {
    /**
     * The in-memory list of all folders for the current world
     */
    private final List<Folder> folders = new ArrayList<>();
    
    /**
     * The storage manager responsible for persisting folders to disk
     */
    private final StorageManager storageManager;
    
    /**
     * Constructs a new FolderManager and loads any existing folders from storage.
     */
    public FolderManager() {
        this.storageManager = new StorageManager();
        loadFolders();
    }
    
    /**
     * Gets all folders managed by this FolderManager.
     * 
     * @return An unmodifiable list of all folders
     */
    public List<Folder> getFolders() {
        return folders;
    }
    
    /**
     * Gets the currently active folder
     * 
     * @return Optional containing the active folder, or empty if no folder is active
     */
    public Optional<Folder> getActiveFolder() {
        Optional<Folder> activeFolder = folders.stream().filter(Folder::isActive).findFirst();
        
        if (activeFolder.isPresent()) {
            com.enoughfolders.util.DebugLogger.debugValue(
                com.enoughfolders.util.DebugLogger.Category.FOLDER_MANAGER,
                "getActiveFolder called, result: {}", 
                activeFolder.get().getName()
            );
        } else {
            com.enoughfolders.util.DebugLogger.debug(
                com.enoughfolders.util.DebugLogger.Category.FOLDER_MANAGER,
                "getActiveFolder called, result: none"
            );
        }
        
        return activeFolder;
    }
    
    /**
     * Sets a folder as active and deactivates all other folders.
     * 
     * @param folder The folder to set as active
     */
    public void setActiveFolder(Folder folder) {
        folders.forEach(f -> f.setActive(f.equals(folder)));
        saveFolders();
    }
    
    /**
     * Deactivates all folders.
     */
    public void clearActiveFolder() {
        folders.forEach(f -> f.setActive(false));
        saveFolders();
    }
    
    /**
     * Creates a new folder with the given name.
     * 
     * @param name The name of the folder to create
     * @return The newly created folder
     */
    public Folder createFolder(String name) {
        Folder folder = new Folder(name);
        folders.add(folder);
        saveFolders();
        return folder;
    }
    
    /**
     * Deletes a folder and all its contents.
     * 
     * @param folder The folder to delete
     */
    public void deleteFolder(Folder folder) {
        folders.remove(folder);
        saveFolders();
    }
    
    /**
     * Adds an ingredient to a specific folder.
     * 
     * @param folder The folder to add the ingredient to
     * @param ingredient The ingredient to add
     */
    public void addIngredient(Folder folder, StoredIngredient ingredient) {
        folder.addIngredient(ingredient);
        saveFolders();
    }
    
    /**
     * Removes an ingredient from a specific folder.
     * 
     * @param folder The folder to remove the ingredient from
     * @param ingredient The ingredient to remove
     */
    public void removeIngredient(Folder folder, StoredIngredient ingredient) {
        folder.removeIngredient(ingredient);
        saveFolders();
    }
    
    /**
     * Reloads folders from disk based on the current world.
     */
    public void reloadFolders() {
        //bugfix
        com.enoughfolders.util.DebugLogger.debug(
            com.enoughfolders.util.DebugLogger.Category.FOLDER_MANAGER,
            "Explicitly reloading folders from disk"
        );
        loadFolders();
    }
    
    /**
     * Loads folders from storage for the current world.
     */
    private void loadFolders() {
        com.enoughfolders.util.DebugLogger.debug(
            com.enoughfolders.util.DebugLogger.Category.FOLDER_MANAGER,
            "Loading folders..."
        );
        folders.clear();
        
        try {
            String worldName = getCurrentWorldName();
            com.enoughfolders.util.DebugLogger.debugValue(
                com.enoughfolders.util.DebugLogger.Category.FOLDER_MANAGER,
                "Current world name: {}", 
                worldName
            );
            
            List<Folder> loadedFolders = storageManager.loadFolders(worldName);
            if (loadedFolders != null) {
                folders.addAll(loadedFolders);
                com.enoughfolders.util.DebugLogger.debugValue(
                    com.enoughfolders.util.DebugLogger.Category.FOLDER_MANAGER,
                    "Loaded {} folders", 
                    folders.size()
                );
                for (Folder folder : folders) {
                    com.enoughfolders.util.DebugLogger.debugValues(
                        com.enoughfolders.util.DebugLogger.Category.FOLDER_MANAGER,
                        "  - Folder: {} (active: {}, ingredients: {})", 
                        folder.getName(), 
                        folder.isActive(), 
                        folder.getIngredients().size()
                    );
                }
            } else {
                com.enoughfolders.util.DebugLogger.debug(
                    com.enoughfolders.util.DebugLogger.Category.FOLDER_MANAGER,
                    "No folders loaded (null list returned)"
                );
            }
        } catch (Exception e) {
            com.enoughfolders.util.DebugLogger.debugValue(
                com.enoughfolders.util.DebugLogger.Category.FOLDER_MANAGER,
                "Error loading folders: {}", 
                e.getMessage()
            );
            EnoughFolders.LOGGER.error("Failed to load folders", e);
        }
    }
    
    /**
     * Saves the current folders to storage for the current world.
     */
    private void saveFolders() {
        try {
            storageManager.saveFolders(getCurrentWorldName(), folders);
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to save folders", e);
        }
    }
    
    /**
     * Determines the current world name to use for saving/loading folders.
     * 
     * @return A unique identifier for the current world
     */
    private String getCurrentWorldName() {
        Minecraft minecraft = Minecraft.getInstance();
        
        // If we're not in a world yet or the player isn't loaded, don't try to load folders
        if (minecraft.level == null || minecraft.player == null) {
            com.enoughfolders.util.DebugLogger.debug(
                com.enoughfolders.util.DebugLogger.Category.FOLDER_MANAGER,
                "Level or player not loaded yet, using default"
            );
            return "default";
        }
        
        // For multiplayer, use server name/ip
        if (!minecraft.isLocalServer()) {
            String serverName = minecraft.getCurrentServer() != null ? 
                minecraft.getCurrentServer().name : "unknown_server";
            String worldName = "mp_" + serverName.replaceAll("[^a-zA-Z0-9_-]", "_");
            com.enoughfolders.util.DebugLogger.debugValue(
                com.enoughfolders.util.DebugLogger.Category.FOLDER_MANAGER,
                "Using multiplayer world name: {}", 
                worldName
            );
            return worldName;
        }
        
        // For single player, use world name
        try {
            if (minecraft.getSingleplayerServer() != null) {
                // Get the save directory path
                java.nio.file.Path savesDir = minecraft.getSingleplayerServer().getWorldPath(
                    net.minecraft.world.level.storage.LevelResource.ROOT).getParent();
                
                if (savesDir != null) {
                    // The actual world name is the last part of the path
                    String worldName = savesDir.getFileName().toString();
                    com.enoughfolders.util.DebugLogger.debugValue(
                        com.enoughfolders.util.DebugLogger.Category.FOLDER_MANAGER,
                        "Using singleplayer world name: {}", 
                        worldName
                    );
                    return worldName;
                }
            }
            
            // Last resort - use a unique identifier based on dimension and spawn point
            if (minecraft.level != null) {
                String dimensionKey = minecraft.level.dimension().location().toString();
                int spawnX = (int) minecraft.player.getX();
                int spawnZ = (int) minecraft.player.getZ();
                String levelId = "world_" + dimensionKey + "_" + Math.abs((spawnX * 31 + spawnZ) % 1000000);
                com.enoughfolders.util.DebugLogger.debugValue(
                    com.enoughfolders.util.DebugLogger.Category.FOLDER_MANAGER,
                    "Using derived level ID: {}", 
                    levelId
                );
                return levelId;
            }
            
            com.enoughfolders.util.DebugLogger.debug(
                com.enoughfolders.util.DebugLogger.Category.FOLDER_MANAGER,
                "Could not determine world name, using default"
            );
            return "default";
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to get world name", e);
            return "default";
        }
    }
}
