package com.enoughfolders.integrations.jei.drag;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.jei.core.JEIRuntimeManager;
import com.enoughfolders.integrations.jei.ingredient.JEIIngredientManager;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Manages JEI ingredient drag operations.
 */
public class JEIDragManager {
    
    /**
     * The JEI runtime manager
     */
    private final JEIRuntimeManager runtimeManager;
    
    /**
     * The JEI ingredient manager
     */
    private final JEIIngredientManager ingredientManager;
    
    /**
     * Currently tracked dragged ingredient
     */
    private Object currentDraggedObject = null;
    
    /**
     * Whether an ingredient is currently being dragged
     */
    private boolean isDragging = false;
    
    /**
     * Creates a new JEI drag manager.
     *
     * @param runtimeManager The JEI runtime manager
     * @param ingredientManager The JEI ingredient manager
     */
    public JEIDragManager(JEIRuntimeManager runtimeManager, JEIIngredientManager ingredientManager) {
        this.runtimeManager = runtimeManager;
        this.ingredientManager = ingredientManager;
    }
    
    /**
     * Gets the ingredient currently being dragged in JEI, if any.
     *
     * @return Optional containing the dragged ingredient, or empty if none is being dragged
     */
    public Optional<Object> getDraggedIngredient() {
        // If we're tracking a dragged object, return that first
        if (isDragging && currentDraggedObject != null) {
            EnoughFolders.LOGGER.info("Returning tracked dragged ingredient: {}", 
                    currentDraggedObject.getClass().getSimpleName());
            return Optional.of(currentDraggedObject);
        }
        
        // Otherwise, try to get it from JEI directly
        if (!runtimeManager.hasRuntime()) {
            EnoughFolders.LOGGER.debug("JEI runtime not available");
            return Optional.empty();
        }
        
        try {
            // Try to get the dragged ingredient from JEI's GhostIngredientDrag
            Optional<Object> draggedIngredient = getJeiDraggedItem();
            draggedIngredient.ifPresent(ingredient -> 
                EnoughFolders.LOGGER.info("Got JEI dragged ingredient: {}", 
                    ingredient.getClass().getSimpleName()));
            return draggedIngredient;
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error getting dragged ingredient", e);
            return Optional.empty();
        }
    }
    
    /**
     * Sets the current dragged object.
     *
     * @param ingredient The ingredient being dragged
     */
    public void setCurrentDraggedObject(Object ingredient) {
        this.currentDraggedObject = ingredient;
        this.isDragging = true;
        EnoughFolders.LOGGER.info("Set current dragged object: {}", 
                ingredient != null ? ingredient.getClass().getSimpleName() : "null");
    }
    
    /**
     * Clears the current dragged object.
     */
    public void clearCurrentDraggedObject() {
        this.currentDraggedObject = null;
        this.isDragging = false;
        EnoughFolders.LOGGER.info("Cleared current dragged object");
    }
    
    /**
     * Helper method to get the dragged ingredient directly from JEI.
     *
     * @return Optional containing the dragged ingredient, or empty if none is found
     */
    private Optional<Object> getJeiDraggedItem() {
        try {
            // Try to get access to JEI's drag manager
            Optional<Object> dragManagerOpt = getJeiDragManager();
            if (dragManagerOpt.isEmpty()) {
                return Optional.empty();
            }
            
            Object dragManager = dragManagerOpt.get();
            EnoughFolders.LOGGER.debug("Found JEI drag manager: {}", dragManager.getClass().getName());
            
            // Try to get the dragged ingredient
            Optional<Object> ingredientOpt = getDraggedIngredientFromManager(dragManager);
            if (ingredientOpt.isPresent()) {
                Object ingredient = ingredientOpt.get();
                EnoughFolders.LOGGER.debug("Found JEI dragged ingredient: {}", ingredient.getClass().getName());
                
                // If it's an ITypedIngredient, extract the actual ingredient
                if (ingredient instanceof mezz.jei.api.ingredients.ITypedIngredient<?> typedIngredient) {
                    return Optional.ofNullable(typedIngredient.getIngredient());
                }
                
                return Optional.of(ingredient);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error getting JEI dragged ingredient", e);
            return Optional.empty();
        }
    }
    
    /**
     * Gets JEI's GhostIngredientDrag manager through various means.
     *
     * @return Optional containing the drag manager, or empty if not found
     */
    private Optional<Object> getJeiDragManager() {
        // First try through minecraft GUI screen if JEI overlay is active
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            try {
                // JEI often adds its drag manager as a listener to GUI screens
                Class<?> screenClass = minecraft.screen.getClass();
                Field listenersField = null;
                
                // Look for any field that might contain listeners
                for (Field field : screenClass.getDeclaredFields()) {
                    if (field.getType().isAssignableFrom(java.util.List.class)) {
                        field.setAccessible(true);
                        java.util.List<?> listeners = (java.util.List<?>) field.get(minecraft.screen);
                        
                        if (listeners != null) {
                            for (Object listener : listeners) {
                                if (listener != null && listener.getClass().getSimpleName().contains("GhostIngredientDrag")) {
                                    return Optional.of(listener);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                EnoughFolders.LOGGER.debug("Error finding drag manager in screen: {}", e.getMessage());
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Gets the dragged ingredient from JEI's drag manager.
     *
     * @param dragManager The JEI drag manager object
     * @return Optional containing the dragged ingredient, or empty if none is found
     */
    private Optional<Object> getDraggedIngredientFromManager(Object dragManager) {
        try {
            // Try to find and invoke the getter method for the dragged ingredient
            for (Method method : dragManager.getClass().getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && 
                    (method.getName().contains("get") || method.getName().contains("drag")) && 
                    !method.getReturnType().equals(void.class)) {
                    
                    method.setAccessible(true);
                    Object result = method.invoke(dragManager);
                    
                    // If result is Optional, unwrap it
                    if (result instanceof Optional<?> opt) {
                        if (opt.isPresent()) {
                            return Optional.of(opt.get());
                        }
                    } 
                    // If result is directly an ingredient
                    else if (result != null && 
                             (result instanceof mezz.jei.api.ingredients.ITypedIngredient<?> || 
                              !result.getClass().getName().contains("java.lang"))) {
                        return Optional.of(result);
                    }
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.debug("Error extracting ingredient from drag manager: {}", e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Processes a drop of the currently dragged ingredient onto a folder.
     * 
     * @param folder The folder to add the ingredient to
     * @return True if the drop was successful, false otherwise
     */
    public boolean handleIngredientDrop(Folder folder) {
        Optional<Object> draggedIngredient = getDraggedIngredient();
        if (draggedIngredient.isEmpty()) {
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "No ingredient is being dragged");
            return false;
        }
        
        Object ingredient = draggedIngredient.get();
        DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
            "Processing JEI dragged ingredient drop for folder: {}", folder.getName());
        
        // Convert ingredient to StoredIngredient
        Optional<StoredIngredient> storedIngredient = ingredientManager.storeIngredient(ingredient);
        if (storedIngredient.isEmpty()) {
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "Failed to convert ingredient to StoredIngredient");
            return false;
        }
        
        // Add ingredient to folder
        EnoughFolders.getInstance().getFolderManager().addIngredient(folder, storedIngredient.get());
        
        // Clear the dragged ingredient so it doesn't get processed again
        clearCurrentDraggedObject();
        
        DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
            "Successfully added JEI ingredient to folder: {}", folder.getName());
        return true;
    }
}
