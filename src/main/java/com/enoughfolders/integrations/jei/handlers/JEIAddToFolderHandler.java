package com.enoughfolders.integrations.jei.handlers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.common.handlers.BaseAddToFolderHandler;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import com.enoughfolders.di.IntegrationProviderRegistry;
import com.enoughfolders.util.DebugLogger;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IIngredientListOverlay;

import java.util.Optional;

/**
 * JEI-specific handler for adding ingredients to folders via keyboard shortcuts.
 */
public class JEIAddToFolderHandler extends BaseAddToFolderHandler {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private JEIAddToFolderHandler() {
        super();
    }

    /**
     * Handles the key press to add a JEI ingredient to the active folder.
     */
    public static void handleAddToFolderKeyPress() {
        // Check if JEI classes are available
        if (!checkIntegrationClasses("mezz.jei.api.runtime.IJeiRuntime")) {
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEI classes not found, skipping JEI integration");
            return;
        }

        // Get the JEI integration
        Optional<JEIIntegration> jeiIntegration = getJEIIntegration();
        if (jeiIntegration.isEmpty()) {
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "JEI integration not available");
            return;
        }

        EnoughFolders.LOGGER.info("JEI Add to Folder handler activated");

        // Make sure we have an active folder
        com.enoughfolders.data.FolderManager folderManager = EnoughFolders.getInstance().getFolderManager();
        Optional<com.enoughfolders.data.Folder> activeFolder = folderManager.getActiveFolder();
        if (activeFolder.isEmpty()) {
            // No active folder, display a message to the user
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                EnoughFolders.LOGGER.debug("No active folder available, displaying message to player");
            }
            DebugLogger.debug(DebugLogger.Category.INPUT, 
                "No active folder available for adding ingredient");
            return;
        }

        EnoughFolders.LOGGER.debug("Active folder found: {}", activeFolder.get().getName());

        // First, check if mouse is hovering over a folder ingredient slot for removal
        JEIIngredientRetriever retriever = new JEIIngredientRetriever(jeiIntegration.get());
        Optional<StoredIngredient> hoveredIngredient = retriever.getHoveredFolderIngredient();
        if (hoveredIngredient.isPresent()) {
            // Remove the ingredient from the folder
            folderManager.removeIngredient(activeFolder.get(), hoveredIngredient.get());
            DebugLogger.debugValues(DebugLogger.Category.INPUT, 
                "Removed JEI ingredient from folder '{}'", activeFolder.get().getName());
            
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                EnoughFolders.LOGGER.debug("Ingredient removed from folder, displaying message to player");
            }
            return;
        }

        // If not hovering over folder ingredient, try to add from JEI overlay
        BaseAddToFolderHandler.handleAddToFolderKeyPress("JEI", retriever);
    }

    /**
     * Gets the JEI integration instance.
     */
    private static Optional<JEIIntegration> getJEIIntegration() {
        return IntegrationProviderRegistry.getIntegrationByClassName("com.enoughfolders.integrations.jei.core.JEIIntegration")
            .filter(JEIIntegration.class::isInstance)
            .map(JEIIntegration.class::cast);
    }

    /**
     * JEI-specific implementation of ingredient retrieval operations.
     */
    private static class JEIIngredientRetriever implements IngredientRetriever {
        private final JEIIntegration jeiIntegration;

        public JEIIngredientRetriever(JEIIntegration jeiIntegration) {
            this.jeiIntegration = jeiIntegration;
        }

        @Override
        public Optional<Object> getIngredientUnderCursor() {
            // First try to get ingredient from JEI overlay
            Optional<Object> jeiIngredient = getJEIIngredientUnderCursor();
            if (jeiIngredient.isPresent()) {
                EnoughFolders.LOGGER.debug("JEI ingredient detection - Found ingredient in JEI overlay");
                return jeiIngredient;
            }
            
            // If no JEI ingredient found, check folder GUI
            Optional<StoredIngredient> folderIngredient = getHoveredFolderIngredient();
            if (folderIngredient.isPresent()) {
                EnoughFolders.LOGGER.debug("JEI ingredient detection - Found ingredient in folder GUI, converting back to JEI ingredient");
                // Convert StoredIngredient back to JEI ingredient
                Optional<?> convertedOpt = jeiIntegration.getIngredientFromStored(folderIngredient.get());
                if (convertedOpt.isPresent()) {
                    return Optional.of(convertedOpt.get());
                }
            }
            
            EnoughFolders.LOGGER.debug("JEI ingredient detection - No ingredient found in JEI overlay or folder GUI");
            return Optional.empty();
        }

        @Override
        public Optional<StoredIngredient> convertToStoredIngredient(Object ingredient) {
            try {
                return jeiIntegration.storeIngredient(ingredient);
            } catch (Exception e) {
                EnoughFolders.LOGGER.error("Error converting JEI ingredient to StoredIngredient", e);
                return Optional.empty();
            }
        }

        @Override
        public boolean isOverlayVisible() {
            try {
                Optional<IJeiRuntime> runtimeOpt = jeiIntegration.getJeiRuntime();
                if (runtimeOpt.isEmpty()) {
                    return false;
                }
                
                IJeiRuntime runtime = runtimeOpt.get();
                IIngredientListOverlay overlay = runtime.getIngredientListOverlay();
                return overlay.isListDisplayed();
            } catch (Exception e) {
                EnoughFolders.LOGGER.error("Error checking JEI overlay visibility", e);
                return false;
            }
        }
        
        /**
         * Gets ingredient from JEI overlay specifically.
         */
        private Optional<Object> getJEIIngredientUnderCursor() {
            try {
                Optional<IJeiRuntime> runtimeOpt = jeiIntegration.getJeiRuntime();
                if (runtimeOpt.isEmpty()) {
                    EnoughFolders.LOGGER.debug("JEI runtime not available for ingredient retrieval");
                    return Optional.empty();
                }

                IJeiRuntime runtime = runtimeOpt.get();
                IIngredientListOverlay overlay = runtime.getIngredientListOverlay();

                // Get current mouse position for debugging
                net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                double mouseX = minecraft.mouseHandler.xpos() * 
                    (double)minecraft.getWindow().getGuiScaledWidth() / 
                    (double)minecraft.getWindow().getScreenWidth();
                double mouseY = minecraft.mouseHandler.ypos() * 
                    (double)minecraft.getWindow().getGuiScaledHeight() / 
                    (double)minecraft.getWindow().getScreenHeight();

                EnoughFolders.LOGGER.debug("JEI ingredient detection - Mouse position: ({}, {})", mouseX, mouseY);
                EnoughFolders.LOGGER.debug("JEI ingredient detection - Overlay displayed: {}", overlay.isListDisplayed());

                if (!overlay.isListDisplayed()) {
                    EnoughFolders.LOGGER.debug("JEI ingredient list not displayed");
                    return Optional.empty();
                }

                // Check if there are visible ingredients in the overlay
                try {
                    // Try to get visible ingredients count
                    java.lang.reflect.Method getVisibleIngredientsMethod = overlay.getClass().getMethod("getVisibleIngredients");
                    java.util.Collection<?> visibleIngredients = (java.util.Collection<?>) getVisibleIngredientsMethod.invoke(overlay);
                    EnoughFolders.LOGGER.debug("JEI ingredient detection - Visible ingredients count: {}", 
                        visibleIngredients != null ? visibleIngredients.size() : "null");
                } catch (Exception e) {
                    EnoughFolders.LOGGER.debug("JEI ingredient detection - Could not get visible ingredients count: {}", e.getMessage());
                }

                Optional<ITypedIngredient<?>> ingredientOpt = overlay.getIngredientUnderMouse();
                EnoughFolders.LOGGER.debug("JEI getIngredientUnderMouse returned: {}", 
                    ingredientOpt.isPresent() ? "ingredient found: " + ingredientOpt.get().getIngredient().getClass().getSimpleName() : "no ingredient");
                
                if (ingredientOpt.isEmpty()) {
                    // Try alternative detection methods
                    EnoughFolders.LOGGER.debug("JEI ingredient detection - Trying alternative detection methods");
                    
                    // Check if mouse is within overlay bounds
                    try {
                        java.lang.reflect.Method getAreaMethod = overlay.getClass().getMethod("getArea");
                        net.minecraft.client.gui.navigation.ScreenRectangle area = (net.minecraft.client.gui.navigation.ScreenRectangle) getAreaMethod.invoke(overlay);
                        boolean mouseInBounds = area.containsPoint((int)mouseX, (int)mouseY);
                        EnoughFolders.LOGGER.debug("JEI ingredient detection - Mouse in overlay bounds: {} (area: {})", mouseInBounds, area);
                    } catch (Exception e) {
                        EnoughFolders.LOGGER.debug("JEI ingredient detection - Could not check overlay bounds: {}", e.getMessage());
                    }
                }

                return ingredientOpt.map(ITypedIngredient::getIngredient);
            } catch (Exception e) {
                EnoughFolders.LOGGER.error("Error getting JEI ingredient under cursor", e);
                return Optional.empty();
            }
        }
        
        /**
         * Gets the folder ingredient currently under the mouse cursor.
         * Based on REI implementation but adapted for JEI.
         */
        public Optional<StoredIngredient> getHoveredFolderIngredient() {
            try {
                // Get current mouse position scaled to GUI coordinates
                net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                double mouseX = minecraft.mouseHandler.xpos() * 
                    (double)minecraft.getWindow().getGuiScaledWidth() / 
                    (double)minecraft.getWindow().getScreenWidth();
                double mouseY = minecraft.mouseHandler.ypos() * 
                    (double)minecraft.getWindow().getGuiScaledHeight() / 
                    (double)minecraft.getWindow().getScreenHeight();
                
                EnoughFolders.LOGGER.debug("JEI folder ingredient detection - Mouse position: ({}, {})", (int)mouseX, (int)mouseY);
                
                // Get the current folder screen
                Optional<com.enoughfolders.client.gui.FolderScreen> folderScreenOpt = getCurrentFolderScreen();
                if (folderScreenOpt.isEmpty()) {
                    EnoughFolders.LOGGER.debug("JEI folder ingredient detection - No folder screen available for hover detection");
                    return Optional.empty();
                }
                
                com.enoughfolders.client.gui.FolderScreen folderScreen = folderScreenOpt.get();
                EnoughFolders.LOGGER.debug("JEI folder ingredient detection - Found folder screen for hover detection");
                
                // Check if mouse is within folder screen bounds
                boolean isVisible = folderScreen.isVisible(mouseX, mouseY);
                EnoughFolders.LOGGER.debug("JEI folder ingredient detection - Folder screen visibility check: {}", isVisible);
                
                if (!isVisible) {
                    EnoughFolders.LOGGER.debug("JEI folder ingredient detection - Mouse not over folder screen area");
                    return Optional.empty();
                }
                
                // Check all ingredient slots for hover
                var ingredientSlots = folderScreen.getIngredientSlots();
                EnoughFolders.LOGGER.debug("JEI folder ingredient detection - Checking {} ingredient slots for hover", ingredientSlots.size());
                
                for (var slot : ingredientSlots) {
                    boolean isHovered = slot.isHovered((int)mouseX, (int)mouseY);
                    if (isHovered) {
                        EnoughFolders.LOGGER.debug("JEI folder ingredient detection - Found hovered ingredient slot at mouse position ({}, {})", (int)mouseX, (int)mouseY);
                        StoredIngredient ingredient = slot.getIngredient();
                        EnoughFolders.LOGGER.debug("JEI folder ingredient detection - Ingredient in hovered slot: {}", ingredient != null ? ingredient.toString() : "null");
                        return Optional.ofNullable(ingredient);
                    }
                }
                
                EnoughFolders.LOGGER.debug("JEI folder ingredient detection - No ingredient slot hovered at current mouse position");
                return Optional.empty();
                
            } catch (Exception e) {
                EnoughFolders.LOGGER.error("Error detecting hovered folder ingredient", e);
                return Optional.empty();
            }
        }
        
        /**
         * Gets the current folder screen from either container screens or recipe screens.
         */
        private Optional<com.enoughfolders.client.gui.FolderScreen> getCurrentFolderScreen() {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            EnoughFolders.LOGGER.debug("JEI folder screen detection - Current screen: {}", 
                minecraft.screen != null ? minecraft.screen.getClass().getSimpleName() : "null");
            
            // First try container screens
            if (minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen) {
                Optional<com.enoughfolders.client.gui.FolderScreen> containerFolderScreen = 
                    com.enoughfolders.client.event.ClientEventHandler.getFolderScreen(containerScreen);
                if (containerFolderScreen.isPresent()) {
                    EnoughFolders.LOGGER.debug("JEI folder screen detection - Found folder screen from container screen");
                    return containerFolderScreen;
                }
            }
            
            // Try recipe screen handlers if container screens didn't work
            try {
                // Try current screen as recipe screen
                Optional<com.enoughfolders.client.gui.FolderScreen> recipeFolderScreen = 
                    com.enoughfolders.client.event.ClientEventHandler.getRecipeScreen(minecraft.screen);
                if (recipeFolderScreen.isPresent()) {
                    EnoughFolders.LOGGER.debug("JEI folder screen detection - Found folder screen from recipe screen");
                    return recipeFolderScreen;
                }
            } catch (Exception e) {
                EnoughFolders.LOGGER.debug("JEI folder screen detection - Error getting recipe folder screen: {}", e.getMessage());
            }
            
            // Try JEI recipe screen handler as backup
            try {
                Optional<com.enoughfolders.client.gui.FolderScreen> jeiRecipeFolderScreen = 
                    com.enoughfolders.integrations.jei.gui.handlers.RecipeScreenHandler.getLastFolderScreen();
                if (jeiRecipeFolderScreen.isPresent()) {
                    EnoughFolders.LOGGER.debug("JEI folder screen detection - Found folder screen from JEI recipe screen handler");
                    return jeiRecipeFolderScreen;
                }
            } catch (Exception e) {
                EnoughFolders.LOGGER.debug("JEI folder screen detection - Error getting JEI recipe folder screen: {}", e.getMessage());
            }
            
            EnoughFolders.LOGGER.debug("JEI folder screen detection - No folder screen found");
            return Optional.empty();
        }
    }
}
