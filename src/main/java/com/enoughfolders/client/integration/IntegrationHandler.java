package com.enoughfolders.client.integration;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.client.gui.IngredientSlot;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.api.FolderTarget;
import com.enoughfolders.integrations.api.RecipeViewingIntegration;
import com.enoughfolders.integrations.jei.gui.targets.FolderButtonTarget;
import com.enoughfolders.integrations.rei.gui.targets.REIFolderTarget;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A centralized handler for recipe viewing mod integrations (JEI, REI, etc.).
 * Provides a unified API for managing different recipe viewing integrations.
 */
public class IntegrationHandler {

    /**
     * Integration priority order, highest priority first.
     */
    private static final String[] PRIORITY_ORDER = {
        "rei",
        "jei"
    };

    private final FolderScreen folderScreen;

    /**
     * Creates a new integration handler for the given folder screen.
     *
     * @param folderScreen The folder screen to handle integrations for
     */
    public IntegrationHandler(FolderScreen folderScreen) {
        this.folderScreen = folderScreen;
    }

    /**
     * Initializes integrations for the folder screen.
     *
     * @param containerScreen The container screen that contains the folder screen
     */
    public void initIntegrations(AbstractContainerScreen<?> containerScreen) {
        for (String integrationId : PRIORITY_ORDER) {
            tryConnectToFolderScreen(integrationId, containerScreen);
        }
    }

    /**
     * Attempts to connect a specific integration to the folder screen.
     *
     * @param integrationId The ID of the integration to connect
     * @param containerScreen The container screen that contains the folder screen
     * @return true if connected successfully, false otherwise
     */
    private boolean tryConnectToFolderScreen(String integrationId, AbstractContainerScreen<?> containerScreen) {
        Optional<RecipeViewingIntegration> integration = getRecipeViewingIntegration(integrationId);
        
        if (integration.isPresent()) {
            RecipeViewingIntegration recipeViewer = integration.get();
            recipeViewer.connectToFolderScreen(folderScreen, containerScreen);
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Connected folder screen to {} integration", recipeViewer.getDisplayName());
            return true;
        }
        
        return false;
    }

    /**
     * Shows recipes for an ingredient using the highest priority available integration.
     *
     * @param ingredient The ingredient to show recipes for
     * @return true if recipes were shown, false otherwise
     */
    public boolean showRecipes(StoredIngredient ingredient) {
        // Try integrations in priority order
        for (String integrationId : PRIORITY_ORDER) {
            if (tryShowRecipes(integrationId, ingredient)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Shows uses for an ingredient using the highest priority available integration.
     *
     * @param ingredient The ingredient to show uses for
     * @return true if uses were shown, false otherwise
     */
    public boolean showUses(StoredIngredient ingredient) {
        // Try integrations in priority order
        for (String integrationId : PRIORITY_ORDER) {
            if (tryShowUses(integrationId, ingredient)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to show recipes for an ingredient using a specific integration.
     *
     * @param integrationId The ID of the integration to use
     * @param ingredient The ingredient to show recipes for
     * @return true if recipes were shown, false otherwise
     */
    private boolean tryShowRecipes(String integrationId, StoredIngredient ingredient) {
        try {
            if ("rei".equals(integrationId)) {
                return IntegrationRegistry.getIntegrationByClassName("com.enoughfolders.integrations.rei.core.REIIntegration")
                    .filter(integration -> integration.isAvailable())
                    .map(rei -> {
                        try {
                            // We need to cast to access the getIngredientFromStored method which isn't in the interface
                            com.enoughfolders.integrations.rei.core.REIIntegration reiIntegration = 
                                (com.enoughfolders.integrations.rei.core.REIIntegration) rei;
                            Optional<?> reiIngredient = reiIntegration.getIngredientFromStored(ingredient);
                            if (reiIngredient.isPresent()) {
                                reiIntegration.showRecipes(reiIngredient.get());
                                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                                    "Showed recipes for ingredient using REI");
                                return true;
                            }
                        } catch (Exception e) {
                            EnoughFolders.LOGGER.error("Error showing recipes with REI", e);
                        }
                        return false;
                    }).orElse(false);
            } else if ("jei".equals(integrationId)) {
                return IntegrationRegistry.getIntegrationByClassName("com.enoughfolders.integrations.jei.core.JEIIntegrationCore")
                    .filter(integration -> integration.isAvailable())
                    .map(jei -> {
                        try {
                            // We need to cast to access the getIngredientFromStored method which isn't in the interface
                            com.enoughfolders.integrations.jei.core.JEIIntegrationCore jeiIntegration = 
                                (com.enoughfolders.integrations.jei.core.JEIIntegrationCore) jei;
                            Optional<?> jeiIngredient = jeiIntegration.getIngredientFromStored(ingredient);
                            if (jeiIngredient.isPresent()) {
                                jeiIntegration.showRecipes(jeiIngredient.get());
                                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                                    "Showed recipes for ingredient using JEI");
                                return true;
                            }
                        } catch (Exception e) {
                            EnoughFolders.LOGGER.error("Error showing recipes with JEI", e);
                        }
                        return false;
                    }).orElse(false);
            }
            return false;
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error in tryShowRecipes", e);
            return false;
        }
    }

    /**
     * Attempts to show uses for an ingredient using a specific integration.
     *
     * @param integrationId The ID of the integration to use
     * @param ingredient The ingredient to show uses for
     * @return true if uses were shown, false otherwise
     */
    private boolean tryShowUses(String integrationId, StoredIngredient ingredient) {
        try {
            if ("rei".equals(integrationId)) {
                return IntegrationRegistry.getIntegrationByClassName("com.enoughfolders.integrations.rei.core.REIIntegration")
                    .filter(integration -> integration.isAvailable())
                    .map(rei -> {
                        try {
                            // We need to cast to access the getIngredientFromStored method which isn't in the interface
                            com.enoughfolders.integrations.rei.core.REIIntegration reiIntegration = 
                                (com.enoughfolders.integrations.rei.core.REIIntegration) rei;
                            Optional<?> reiIngredient = reiIntegration.getIngredientFromStored(ingredient);
                            if (reiIngredient.isPresent()) {
                                reiIntegration.showUses(reiIngredient.get());
                                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                                    "Showed uses for ingredient using REI");
                                return true;
                            }
                        } catch (Exception e) {
                            EnoughFolders.LOGGER.error("Error showing uses with REI", e);
                        }
                        return false;
                    }).orElse(false);
            } else if ("jei".equals(integrationId)) {
                return IntegrationRegistry.getIntegrationByClassName("com.enoughfolders.integrations.jei.core.JEIIntegrationCore")
                    .filter(integration -> integration.isAvailable())
                    .map(jei -> {
                        try {
                            // We need to cast to access the getIngredientFromStored method which isn't in the interface
                            com.enoughfolders.integrations.jei.core.JEIIntegrationCore jeiIntegration = 
                                (com.enoughfolders.integrations.jei.core.JEIIntegrationCore) jei;
                            Optional<?> jeiIngredient = jeiIntegration.getIngredientFromStored(ingredient);
                            if (jeiIngredient.isPresent()) {
                                jeiIntegration.showUses(jeiIngredient.get());
                                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                                    "Showed uses for ingredient using JEI");
                                return true;
                            }
                        } catch (Exception e) {
                            EnoughFolders.LOGGER.error("Error showing uses with JEI", e);
                        }
                        return false;
                    }).orElse(false);
            }
            return false;
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error in tryShowUses", e);
            return false;
        }
    }

    /**
     * Handles a click on an ingredient slot.
     *
     * @param slot The slot that was clicked
     * @param button The mouse button used (0 = left, 1 = right)
     * @param shift Whether shift was held
     * @param ctrl Whether ctrl was held
     * @return true if the click was handled, false otherwise
     */
    public boolean handleIngredientClick(IngredientSlot slot, int button, boolean shift, boolean ctrl) {
        if (!slot.hasIngredient()) {
            return false;
        }
        
        StoredIngredient ingredient = slot.getIngredient();
        
        // Left click for recipes, right click for uses
        if (button == 0) {
            return showRecipes(ingredient);
        } else if (button == 1) {
            return showUses(ingredient);
        }
        
        return false;
    }

    /**
     * Checks if the given screen is a recipe viewing screen for any integration.
     *
     * @param screen The screen to check
     * @return true if it's a recipe viewing screen, false otherwise
     */
    public boolean isRecipeScreen(Screen screen) {
        for (String integrationId : PRIORITY_ORDER) {
            Optional<RecipeViewingIntegration> integration = getRecipeViewingIntegration(integrationId);
            if (integration.isPresent() && integration.get().isRecipeScreen(screen)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the screen being closed is transitioning to a recipe viewing screen.
     *
     * @param screen The screen being closed
     * @return true if transitioning to a recipe viewing screen, false otherwise
     */
    public boolean isTransitioningToRecipeScreen(Screen screen) {
        for (String integrationId : PRIORITY_ORDER) {
            Optional<RecipeViewingIntegration> integration = getRecipeViewingIntegration(integrationId);
            if (integration.isPresent() && integration.get().isTransitioningToRecipeScreen(screen)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets folder targets for all folder buttons, using the available recipe viewing integration.
     *
     * @param folderButtons The folder buttons to get targets for
     * @return List of folder targets for the available recipe viewing integration
     */
    public List<FolderButtonTarget> getFolderTargets(List<FolderButton> folderButtons) {
        // This method returns JEI targets to match the interface required by FolderGhostIngredientTarget
        
        // Try REI first
        if (isIntegrationAvailable("rei")) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "Using REI folder targets");
            // When REI is present, still return JEI targets since the interface requires FolderButtonTarget
            return getJEIFolderTargets(folderButtons);
        }
        
        // Try JEI
        if (isIntegrationAvailable("jei")) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "Using JEI folder targets");
            return getJEIFolderTargets(folderButtons);
        }
        
        // No recipe viewing integration available
        DebugLogger.debug(DebugLogger.Category.INTEGRATION, "No recipe viewing integration available");
        return new ArrayList<>();
    }

    /**
     * Gets folder targets with the specified target type, using available recipe viewing integrations.
     *
     * @param <T> The type of folder targets to return
     * @param folderButtons The folder buttons to get targets for
     * @return List of typed folder targets for the available recipe viewing integration
     */
    @SuppressWarnings("unchecked")
    public <T extends FolderTarget> List<T> getTypedFolderTargets(List<FolderButton> folderButtons) {
        // Try REI first
        if (isIntegrationAvailable("rei")) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "Using REI folder targets");
            return (List<T>) getREIFolderTargets(folderButtons);
        }
        
        // Try JEI
        if (isIntegrationAvailable("jei")) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "Using JEI folder targets");
            return (List<T>) getJEIFolderTargets(folderButtons);
        }
        
        // No recipe viewing integration available
        DebugLogger.debug(DebugLogger.Category.INTEGRATION, "No recipe viewing integration available");
        return new ArrayList<>();
    }

    /**
     * Gets JEI-specific folder targets for the given folder buttons.
     *
     * @param folderButtons The folder buttons to get targets for
     * @return List of JEI folder button targets
     */
    public List<FolderButtonTarget> getJEIFolderTargets(List<FolderButton> folderButtons) {
        EnoughFolders.LOGGER.debug("Building JEI folder targets - Number of folder buttons available: {}", folderButtons.size());
        return getFolderTargets("jei", folderButtons);
    }

    /**
     * Gets REI-specific folder targets for the given folder buttons.
     *
     * @param folderButtons The folder buttons to get targets for
     * @return List of REI folder targets
     */
    public List<REIFolderTarget> getREIFolderTargets(List<FolderButton> folderButtons) {
        EnoughFolders.LOGGER.debug("Building REI folder targets - Number of folder buttons available: {}", folderButtons.size());
        return getFolderTargets("rei", folderButtons);
    }

    /**
     * Gets folder targets for a specific integration.
     *
     * @param <T> The type of folder targets to return
     * @param integrationId The ID of the integration to use
     * @param folderButtons The folder buttons to get targets for
     * @return List of folder targets, or an empty list if the integration is not available
     */
    @SuppressWarnings("unchecked")
    private <T extends FolderTarget> List<T> getFolderTargets(String integrationId, List<FolderButton> folderButtons) {
        return getRecipeViewingIntegration(integrationId)
            .map(integration -> {
                List<?> targets = integration.createFolderTargets(folderButtons);
                // Cast the targets to the requested type
                return (List<T>) targets;
            })
            .orElse(new ArrayList<>());
    }

    // Deprecated handleIngredientDrop method removed - use handleIngredientDrop(double, double, List<FolderButton>) instead

    /**
     * Attempts to handle an ingredient drop using a specific integration.
     *
     * @param integrationId The ID of the integration to use
     * @param folder The folder to drop the ingredient onto
     * @return true if the drop was handled, false otherwise
     */
    private boolean tryHandleIngredientDrop(String integrationId, Folder folder) {
        try {
            String className = null;
            
            if ("jei".equals(integrationId)) {
                className = "com.enoughfolders.integrations.jei.core.JEIIntegrationCore";
            } else if ("rei".equals(integrationId)) {
                className = "com.enoughfolders.integrations.rei.core.REIIntegration";
            }
            
            if (className == null) {
                return false;
            }
            
            return IntegrationRegistry.getIntegrationByClassName(className)
                .filter(integration -> integration instanceof com.enoughfolders.integrations.api.IngredientDragProvider)
                .map(integration -> (com.enoughfolders.integrations.api.IngredientDragProvider) integration)
                .filter(dragProvider -> dragProvider.isAvailable())
                .map(dragProvider -> dragProvider.handleIngredientDrop(folder))
                .orElse(false);
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error handling ingredient drop for " + integrationId, e);
            return false;
        }
    }

    /**
     * Process an ingredient drop at the given mouse position.
     * Determines if the mouse is hovering over any folder and handles the drop.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param folderButtons The list of folder buttons to check
     * @return true if the drop was handled, false otherwise
     */
    public boolean handleIngredientDrop(double mouseX, double mouseY, List<FolderButton> folderButtons) {
        DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
            "Processing ingredient drop at {},{} with {} folder buttons", 
            mouseX, mouseY, folderButtons.size());
            
        // Find the folder button under the cursor
        for (FolderButton button : folderButtons) {
            if (button.isPointInButton((int)mouseX, (int)mouseY)) {
                // If the mouse is over a folder button, try to drop onto that folder
                Folder folder = button.getFolder();
                
                // Try integrations in priority order
                for (String integrationId : PRIORITY_ORDER) {
                    if (tryHandleIngredientDrop(integrationId, folder)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * Gets a RecipeViewingIntegration for the given integration ID.
     *
     * @param integrationId The ID of the integration to get
     * @return Optional containing the integration, or empty if not available
     */
    private Optional<RecipeViewingIntegration> getRecipeViewingIntegration(String integrationId) {
        String className = null;
        
        if ("jei".equals(integrationId)) {
            className = "com.enoughfolders.integrations.jei.core.JEIIntegrationCore";
        } else if ("rei".equals(integrationId)) {
            className = "com.enoughfolders.integrations.rei.core.REIIntegration";
        }
        
        if (className == null) {
            return Optional.empty();
        }
        
        return IntegrationRegistry.getIntegrationByClassName(className)
            .filter(integration -> integration instanceof RecipeViewingIntegration)
            .map(integration -> (RecipeViewingIntegration) integration)
            .filter(RecipeViewingIntegration::isAvailable);
    }

    /**
     * Checks if an integration is available.
     *
     * @param integrationId The ID of the integration to check
     * @return true if the integration is available, false otherwise
     */
    private boolean isIntegrationAvailable(String integrationId) {
        return getRecipeViewingIntegration(integrationId).isPresent();
    }
    
    /**
     * Checks if any recipe viewing integration is available.
     *
     * @return true if at least one recipe viewing integration is available, false otherwise
     */
    public boolean isAnyIntegrationAvailable() {
        for (String integrationId : PRIORITY_ORDER) {
            if (isIntegrationAvailable(integrationId)) {
                return true;
            }
        }
        return false;
    }
}
