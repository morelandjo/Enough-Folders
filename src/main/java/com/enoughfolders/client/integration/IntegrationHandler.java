package com.enoughfolders.client.integration;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.client.gui.IngredientSlot;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.di.DependencyProvider;
import com.enoughfolders.integrations.api.RecipeViewingIntegration;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import com.enoughfolders.integrations.rei.core.REIIntegration;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.Optional;

/**
 * A centralized handler for recipe viewing mod integrations.
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
        if (folderScreen == null) {
            EnoughFolders.LOGGER.error("Cannot initialize integrations: folder screen is null");
            return;
        }

        // Try to connect to integrations in priority order
        for (String integrationId : PRIORITY_ORDER) {
            if (tryConnectToFolderScreen(integrationId, containerScreen)) {
                DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                    "Connected folder screen to {} integration", integrationId);
                return;
            }
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
                return DependencyProvider.get(REIIntegration.class)
                    .filter(integration -> integration.isAvailable())
                    .map(reiIntegration -> {
                        try {
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
                return DependencyProvider.get(JEIIntegration.class)
                    .filter(integration -> integration.isAvailable())
                    .map(jeiIntegration -> {
                        try {
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
                return DependencyProvider.get(REIIntegration.class)
                    .filter(integration -> integration.isAvailable())
                    .map(reiIntegration -> {
                        try {
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
                return DependencyProvider.get(JEIIntegration.class)
                    .filter(integration -> integration.isAvailable())
                    .map(jeiIntegration -> {
                        try {
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
     * Gets a RecipeViewingIntegration for the given integration ID.
     *
     * @param integrationId The ID of the integration to get
     * @return Optional containing the integration, or empty if not available
     */
    private Optional<RecipeViewingIntegration> getRecipeViewingIntegration(String integrationId) {
        if ("jei".equals(integrationId)) {
            return DependencyProvider.get(JEIIntegration.class)
                .map(jei -> (RecipeViewingIntegration) jei)
                .filter(RecipeViewingIntegration::isAvailable);
        } else if ("rei".equals(integrationId)) {
            return DependencyProvider.get(REIIntegration.class)
                .map(rei -> (RecipeViewingIntegration) rei)
                .filter(RecipeViewingIntegration::isAvailable);
        }
        
        return Optional.empty();
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

    /**
     * Gets the folder screen associated with this integration handler.
     * 
     * @return The folder screen
     */
    public FolderScreen getFolderScreen() {
        return folderScreen;
    }
}
