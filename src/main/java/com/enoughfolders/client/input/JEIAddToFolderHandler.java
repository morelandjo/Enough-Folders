package com.enoughfolders.client.input;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import com.enoughfolders.util.DebugLogger;

import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Dedicated handler for adding JEI ingredients to folders via keyboard shortcuts.
 */
public class JEIAddToFolderHandler {

    /**
     * Handles the key press to add a JEI ingredient to the active folder.
     */
    public static void handleAddToFolderKeyPress() {
        EnoughFolders.LOGGER.info("JEI Add to Folder handler activated");
        
        // Make sure we have an active folder
        FolderManager folderManager = EnoughFolders.getInstance().getFolderManager();
        Optional<Folder> activeFolder = folderManager.getActiveFolder();
        if (activeFolder.isEmpty()) {
            // No active folder, display a message to the user
            Minecraft.getInstance().player.displayClientMessage(
                Component.translatable("enoughfolders.message.no_active_folder"), false);
            DebugLogger.debug(DebugLogger.Category.INPUT, "No active folder available for adding ingredient");
            return;
        }
        
        EnoughFolders.LOGGER.debug("Active folder found: {}", activeFolder.get().getName());
        
        // Use the JEI integration to check for ingredients
        IntegrationRegistry.getIntegration(JEIIntegration.class).ifPresent(jeiIntegration -> {
            jeiIntegration.getJeiRuntime().ifPresent(jeiRuntime -> {
                EnoughFolders.LOGGER.debug("JEI runtime found, checking for ingredients under mouse");
                
                // Try to get ingredient under cursor directly from JEI
                tryAddIngredientFromJei(jeiRuntime, activeFolder.get(), folderManager);
            });
        });
    }
    
    /**
     * Attempts to add an ingredient from JEI to the specified folder.
     * 
     * @param jeiRuntime The JEI runtime instance to interact with
     * @param folder The target folder to add the ingredient to
     * @param folderManager The folder manager instance
     */
    private static void tryAddIngredientFromJei(IJeiRuntime jeiRuntime, Folder folder, FolderManager folderManager) {
        // Check if the ingredient overlay is displayed and has an ingredient under the mouse
        if (jeiRuntime.getIngredientListOverlay().isListDisplayed()) {
            EnoughFolders.LOGGER.debug("JEI ingredient list is displayed, checking for ingredient under mouse");
            
            boolean found = false;
            
            // Try using the ingredient list overlay
            Optional<ITypedIngredient<?>> ingredientOpt = jeiRuntime.getIngredientListOverlay().getIngredientUnderMouse();
            if (ingredientOpt.isPresent()) {
                EnoughFolders.LOGGER.info("Found ingredient under mouse in JEI list overlay");
                found = processFoundIngredient(ingredientOpt.get(), folder, folderManager);
            } else {
                EnoughFolders.LOGGER.debug("No ingredient found in JEI list overlay, trying bookmarks");
                
                // Try using the bookmark overlay
                Optional<ITypedIngredient<?>> bookmarkOpt = jeiRuntime.getBookmarkOverlay().getIngredientUnderMouse();
                if (bookmarkOpt.isPresent()) {
                    EnoughFolders.LOGGER.info("Found ingredient under mouse in JEI bookmark overlay");
                    found = processFoundIngredient(bookmarkOpt.get(), folder, folderManager);
                } else {
                    EnoughFolders.LOGGER.debug("No ingredient found in JEI bookmark overlay either");
                }
            }
            
            if (!found) {
                EnoughFolders.LOGGER.debug("No JEI ingredient found under mouse cursor in any overlay");
                
                Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("enoughfolders.message.no_ingredient_under_cursor"), false);
            }
        } else {
            EnoughFolders.LOGGER.debug("JEI ingredient list is not displayed");
           
            Minecraft.getInstance().player.displayClientMessage(
                Component.translatable("enoughfolders.message.jei_not_visible"), false);
        }
    }
    
    /**
     * Processes a found JEI ingredient and adds it to the active folder.
     * 
     * @param typedIngredient The JEI typed ingredient that was found under the mouse
     * @param folder The folder to add the ingredient to
     * @param folderManager The folder manager to use for adding the ingredient
     * @return true if the ingredient was successfully added, false otherwise
     */
    private static boolean processFoundIngredient(ITypedIngredient<?> typedIngredient, Folder folder, FolderManager folderManager) {
        EnoughFolders.LOGGER.info("Processing found JEI ingredient: {}", typedIngredient.getIngredient().getClass().getName());
        
        // Convert the JEI ingredient to a StoredIngredient
        Object rawIngredient = typedIngredient.getIngredient();
        
        // Get the JEI integration to store the ingredient
        Optional<JEIIntegration> jeiIntOpt = IntegrationRegistry.getIntegration(JEIIntegration.class);
        if (jeiIntOpt.isEmpty()) {
            EnoughFolders.LOGGER.error("JEI integration not available when trying to store ingredient");
            return false;
        }
        
        Optional<StoredIngredient> storedIngredientOpt = jeiIntOpt.get().storeIngredient(rawIngredient);
        if (storedIngredientOpt.isEmpty()) {
            EnoughFolders.LOGGER.error("Failed to create StoredIngredient for {}", rawIngredient.getClass().getName());
            return false;
        }
        
        StoredIngredient storedIngredient = storedIngredientOpt.get();
        EnoughFolders.LOGGER.debug("Successfully converted to StoredIngredient: {}", storedIngredient);
        
        // Add the ingredient to the folder
        folderManager.addIngredient(folder, storedIngredient);
        
        // Log the action but don't display a message to the player
        DebugLogger.debugValue(DebugLogger.Category.INPUT, 
            "Added JEI ingredient to folder '{}'", folder.getName());
        
        return true;
    }
}