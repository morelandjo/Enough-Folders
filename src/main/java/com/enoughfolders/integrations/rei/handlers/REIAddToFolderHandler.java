package com.enoughfolders.integrations.rei.handlers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.di.DependencyProvider;
import com.enoughfolders.integrations.rei.core.REIIntegration;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Handles adding ingredients to folders when using REI integration.
 */
public class REIAddToFolderHandler {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private REIAddToFolderHandler() {
        // Utility class should not be instantiated
    }

    /**
     * Adds the currently hovered REI ingredient to the active folder.
     */
    public static void handleAddToFolderKeyPress() {
        EnoughFolders.LOGGER.info("REI Add to Folder handler activated");
        System.out.println("REI HANDLER: Add to Folder handler activated - SYSTEM PRINT");
        
        // Make sure we have an active folder
        FolderManager folderManager = EnoughFolders.getInstance().getFolderManager();
        Optional<Folder> activeFolder = folderManager.getActiveFolder();
        if (activeFolder.isEmpty()) {
            // No active folder, display a message to the user
            var player = Minecraft.getInstance().player;
            if (player != null) {
                EnoughFolders.LOGGER.debug("No active folder available, displaying message to player");
            }
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "No active folder available for adding ingredient");
            return;
        }

        EnoughFolders.LOGGER.debug("Active folder found: {}", activeFolder.get().getName());

        // First, check if mouse is hovering over a folder ingredient slot for removal
        Optional<StoredIngredient> hoveredIngredient = getHoveredFolderIngredient();
        if (hoveredIngredient.isPresent()) {
            // Remove the ingredient from the folder
            folderManager.removeIngredient(activeFolder.get(), hoveredIngredient.get());
            DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION, 
                "Removed REI ingredient from folder '{}'", activeFolder.get().getName());
            
            var player = Minecraft.getInstance().player;
            if (player != null) {
                EnoughFolders.LOGGER.debug("Ingredient removed from folder, displaying message to player");
            }
            return;
        }
        
        try {
            // Check if REI classes exist before doing anything
            System.out.println("REI HANDLER: About to check for REI classes - SYSTEM PRINT");
            Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            System.out.println("REI HANDLER: REI classes found - SYSTEM PRINT");
            
            // Check if REI integration is available via DependencyProvider
            System.out.println("REI HANDLER: About to check DependencyProvider for REI integration - SYSTEM PRINT");
            Optional<REIIntegration> reiIntegrationOpt = DependencyProvider.get(REIIntegration.class)
                .filter(integration -> integration.isAvailable());
                
            if (reiIntegrationOpt.isEmpty()) {
                EnoughFolders.LOGGER.debug("REI integration not available via DependencyProvider");
                System.out.println("REI HANDLER: REI integration not available via DependencyProvider - SYSTEM PRINT");
                return;
            }
            System.out.println("REI HANDLER: REI integration found via DependencyProvider - SYSTEM PRINT");
            
            // Get the REI integration instance
            REIIntegration reiIntegration = reiIntegrationOpt.get();
            System.out.println("REI HANDLER: Got REI integration instance - SYSTEM PRINT");
            
            // Get the ingredient under mouse from REI overlay
            EnoughFolders.LOGGER.info("About to call getIngredientUnderMouse directly");
            EnoughFolders.LOGGER.info("REI integration class: {}", reiIntegration.getClass().getName());
            
            // Call getIngredientUnderMouse directly
            Optional<?> ingredientOpt = reiIntegration.getIngredientUnderMouse();
            EnoughFolders.LOGGER.info("getIngredientUnderMouse direct call completed successfully");
            
            EnoughFolders.LOGGER.info("getIngredientUnderMouse completed, result: {}", 
                ingredientOpt.isPresent() ? "ingredient found" : "no ingredient");
            
            EnoughFolders.LOGGER.debug("REI integration found, checking for ingredients under mouse");
            
            // Now process the ingredient if found
            if (ingredientOpt.isPresent()) {
                Object ingredient = ingredientOpt.get();
                EnoughFolders.LOGGER.info("Found REI ingredient under mouse: {}", 
                    ingredient != null ? ingredient.getClass().getName() : "null");
                
                // Store the ingredient directly
                Optional<StoredIngredient> storedIngredientOpt = reiIntegration.storeIngredient(ingredient);
                System.out.println("REI HANDLER: storeIngredient call completed - SYSTEM PRINT");
                
                if (storedIngredientOpt.isPresent()) {
                    StoredIngredient storedIngredient = storedIngredientOpt.get();
                    EnoughFolders.LOGGER.debug("Successfully converted to StoredIngredient: {}", storedIngredient);
                    
                    // Add the ingredient to the active folder
                    Folder folder = activeFolder.get();
                    folderManager.addIngredient(folder, storedIngredient);
                    
                    // Log the action
                    DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                        "Added ingredient to folder '{}'", folder.getName());
                        
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        player.sendSystemMessage(Component.translatable("enoughfolders.message.ingredient_added"));
                    }
                } else {
                    EnoughFolders.LOGGER.error("Failed to convert REI ingredient to StoredIngredient");
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        player.sendSystemMessage(Component.translatable("enoughfolders.error.conversion_failed"));
                    }
                }
            } else {
                EnoughFolders.LOGGER.debug("No REI ingredient found under mouse cursor");
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("enoughfolders.message.no_ingredient_found"));
                }
            }
        } catch (ClassNotFoundException e) {
            EnoughFolders.LOGGER.debug("REI classes not found, skipping REI integration");
            System.out.println("REI HANDLER: REI classes not found - SYSTEM PRINT: " + e.getMessage());
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error interacting with REI runtime", e);
            System.out.println("REI HANDLER: GENERIC EXCEPTION - SYSTEM PRINT: " + e.getMessage());
            e.printStackTrace();
            
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(Component.translatable("enoughfolders.error.runtime_interaction", "REI"));
            }
        }
    }

    /**
     * Gets the folder ingredient currently under the mouse cursor.
     * 
     * @return Optional containing the StoredIngredient if hovering over one, empty otherwise
     */
    private static Optional<StoredIngredient> getHoveredFolderIngredient() {
        try {
            // Get current mouse position scaled to GUI coordinates
            Minecraft minecraft = Minecraft.getInstance();
            double mouseX = minecraft.mouseHandler.xpos() * 
                (double)minecraft.getWindow().getGuiScaledWidth() / 
                (double)minecraft.getWindow().getScreenWidth();
            double mouseY = minecraft.mouseHandler.ypos() * 
                (double)minecraft.getWindow().getGuiScaledHeight() / 
                (double)minecraft.getWindow().getScreenHeight();
            
            DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION, 
                "Mouse position: ({}, {})", (int)mouseX, (int)mouseY);
            
            // Get the current folder screen
            Optional<com.enoughfolders.client.gui.FolderScreen> folderScreenOpt = getCurrentFolderScreen();
            if (folderScreenOpt.isEmpty()) {
                DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "No folder screen available for hover detection");
                return Optional.empty();
            }
            
            com.enoughfolders.client.gui.FolderScreen folderScreen = folderScreenOpt.get();
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Found folder screen for hover detection");
            
            // Check if mouse is within folder screen bounds
            boolean isVisible = folderScreen.isVisible(mouseX, mouseY);
            DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION, 
                "Folder screen visibility check: {}", isVisible);
            
            if (!isVisible) {
                DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Mouse not over folder screen area");
                return Optional.empty();
            }
            
            // Check all ingredient slots for hover
            var ingredientSlots = folderScreen.getIngredientSlots();
            DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION, 
                "Checking {} ingredient slots for hover", ingredientSlots.size());
            
            for (var slot : ingredientSlots) {
                boolean isHovered = slot.isHovered((int)mouseX, (int)mouseY);
                if (isHovered) {
                    DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION, 
                        "Found hovered ingredient slot at mouse position ({}, {})", (int)mouseX, (int)mouseY);
                    StoredIngredient ingredient = slot.getIngredient();
                    DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION, 
                        "Ingredient in hovered slot: {}", ingredient != null ? ingredient.toString() : "null");
                    return Optional.of(ingredient);
                }
            }
            
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "No ingredient slot hovered at current mouse position");
            return Optional.empty();
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error detecting hovered folder ingredient", e);
            return Optional.empty();
        }
    }

    /**
     * Gets the current folder screen from either container screens or recipe screens.
     * 
     * @return Optional containing the FolderScreen if available, empty otherwise
     */
    private static Optional<com.enoughfolders.client.gui.FolderScreen> getCurrentFolderScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION, 
            "Current screen: {}", minecraft.screen != null ? minecraft.screen.getClass().getSimpleName() : "null");
        
        // First try container screens
        if (minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen) {
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Checking container screen for folder screen");
            var folderScreenOpt = com.enoughfolders.client.event.ClientEventHandler.getFolderScreen(containerScreen);
            if (folderScreenOpt.isPresent()) {
                DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Found folder screen from container screen");
                return folderScreenOpt;
            } else {
                DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "No folder screen found from container screen");
            }
        }
        
        // Then try recipe screens through integrations
        try {
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Checking recipe screen integrations for folder screen");
            
            // Check JEI integration
            var jeiIntegrationOpt = com.enoughfolders.di.DependencyProvider.get(
                com.enoughfolders.integrations.jei.core.JEIIntegration.class);
            if (jeiIntegrationOpt.isPresent()) {
                var jeiIntegration = jeiIntegrationOpt.get();
                if (jeiIntegration.isAvailable()) {
                    var folderScreenOpt = jeiIntegration.getLastFolderScreen();
                    if (folderScreenOpt.isPresent()) {
                        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Found folder screen from JEI integration");
                        return folderScreenOpt;
                    }
                }
            }
            
            // Check REI integration
            var reiIntegrationOpt = com.enoughfolders.di.DependencyProvider.get(
                com.enoughfolders.integrations.rei.core.REIIntegration.class);
            if (reiIntegrationOpt.isPresent()) {
                var reiIntegration = reiIntegrationOpt.get();
                if (reiIntegration.isAvailable()) {
                    var folderScreenOpt = reiIntegration.getLastFolderScreen();
                    if (folderScreenOpt.isPresent()) {
                        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Found folder screen from REI integration");
                        return folderScreenOpt;
                    }
                }
            }
            
            // Check EMI integration
            var emiIntegrationOpt = com.enoughfolders.di.DependencyProvider.get(
                com.enoughfolders.integrations.emi.core.EMIIntegration.class);
            if (emiIntegrationOpt.isPresent()) {
                var emiIntegration = emiIntegrationOpt.get();
                if (emiIntegration.isAvailable()) {
                    var folderScreenOpt = emiIntegration.getLastFolderScreen();
                    if (folderScreenOpt.isPresent()) {
                        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Found folder screen from EMI integration");
                        return folderScreenOpt;
                    }
                }
            }
        } catch (Exception e) {
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "Error accessing integration folder screen: " + e.getMessage());
        }
        
        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "No folder screen found from any source");
        return Optional.empty();
    }
}
