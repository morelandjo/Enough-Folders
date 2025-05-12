package com.enoughfolders.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A test-friendly version of FolderManager that doesn't depend on Minecraft classes.
 * This allows us to test the core functionality without requiring a Minecraft environment.
 */
public class FolderManagerForTest {
    private final List<Folder> folders = new ArrayList<>();
    
    public FolderManagerForTest() {
        // No StorageManager initialization or folder loading for tests
    }
    
    public List<Folder> getFolders() {
        return folders;
    }
    
    public Optional<Folder> getActiveFolder() {
        return folders.stream().filter(Folder::isActive).findFirst();
    }
    
    public void setActiveFolder(Folder folder) {
        folders.forEach(f -> f.setActive(f.equals(folder)));
    }
    
    public Folder createFolder(String name) {
        Folder folder = new Folder(name);
        folders.add(folder);
        return folder;
    }
    
    public void deleteFolder(Folder folder) {
        folders.remove(folder);
    }
    
    public void addIngredient(Folder folder, StoredIngredient ingredient) {
        folder.addIngredient(ingredient);
    }
    
    public void removeIngredient(Folder folder, StoredIngredient ingredient) {
        folder.removeIngredient(ingredient);
    }
}