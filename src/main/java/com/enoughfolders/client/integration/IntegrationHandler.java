package com.enoughfolders.client.integration;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.client.gui.IngredientSlot;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.di.DependencyProvider;
import com.enoughfolders.integrations.api.FolderTarget;
import com.enoughfolders.integrations.api.IngredientDragProvider;
import com.enoughfolders.integrations.api.RecipeViewingIntegration;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import com.enoughfolders.integrations.jei.gui.targets.FolderButtonTarget;
import com.enoughfolders.integrations.rei.core.REIIntegration;
import com.enoughfolders.integrations.rei.gui.targets.REIFolderTarget;
import com.enoughfolders.integrations.emi.core.EMIIntegration;
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
        "jei",
        "emi"
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
            } else if ("emi".equals(integrationId)) {
                return DependencyProvider.get(EMIIntegration.class)
                    .filter(integration -> integration.isAvailable())
                    .map(emiIntegration -> {
                        try {
                            Optional<?> emiIngredient = emiIntegration.getIngredientFromStored(ingredient);
                            if (emiIngredient.isPresent()) {
                                emiIntegration.showRecipes(emiIngredient.get());
                                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                                    "Showed recipes for ingredient using EMI");
                                return true;
                            }
                        } catch (Exception e) {
                            EnoughFolders.LOGGER.error("Error showing recipes with EMI", e);
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
            } else if ("emi".equals(integrationId)) {
                return DependencyProvider.get(EMIIntegration.class)
                    .filter(integration -> integration.isAvailable())
                    .map(emiIntegration -> {
                        try {
                            Optional<?> emiIngredient = emiIntegration.getIngredientFromStored(ingredient);
                            if (emiIngredient.isPresent()) {
                                emiIntegration.showUses(emiIngredient.get());
                                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                                    "Showed uses for ingredient using EMI");
                                return true;
                            }
                        } catch (Exception e) {
                            EnoughFolders.LOGGER.error("Error showing uses with EMI", e);
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
        Optional<RecipeViewingIntegration> integration = getRecipeViewingIntegration(integrationId);
        
        if (integration.isPresent()) {
            RecipeViewingIntegration recipeViewer = integration.get();
            List<? extends FolderTarget> targets = recipeViewer.createFolderTargets(folderButtons);
            
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Created {} folder targets for {} integration", 
                targets.size(), recipeViewer.getDisplayName());
            
            return (List<T>) targets;
        }
        
        return new ArrayList<>();
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
            Optional<IngredientDragProvider> provider = getIngredientDragProvider(integrationId);
            
            return provider
                .filter(IngredientDragProvider::isAvailable)
                .filter(IngredientDragProvider::isIngredientBeingDragged)
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
        // Try to find a folder button at the given coordinates
        for (FolderButton button : folderButtons) {
            if (button.isHovered()) {
                // Try to handle the drop with each integration
                for (String integrationId : PRIORITY_ORDER) {
                    if (tryHandleIngredientDrop(integrationId, button.getFolder())) {
                        return true;
                    }
                }
                break; // Stop after first matching button
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
        } else if ("emi".equals(integrationId)) {
            return DependencyProvider.get(EMIIntegration.class)
                .map(emi -> (RecipeViewingIntegration) emi)
                .filter(RecipeViewingIntegration::isAvailable);
        }
        
        return Optional.empty();
    }

    /**
     * Gets an IngredientDragProvider for the given integration ID.
     *
     * @param integrationId The ID of the integration to get
     * @return Optional containing the provider, or empty if not available
     */
    private Optional<IngredientDragProvider> getIngredientDragProvider(String integrationId) {
        if ("jei".equals(integrationId)) {
            return DependencyProvider.get(JEIIntegration.class)
                .map(jei -> (IngredientDragProvider) jei)
                .filter(IngredientDragProvider::isAvailable);
        } else if ("rei".equals(integrationId)) {
            return DependencyProvider.get(REIIntegration.class)
                .map(rei -> (IngredientDragProvider) rei)
                .filter(IngredientDragProvider::isAvailable);
        } else if ("emi".equals(integrationId)) {
            return DependencyProvider.get(EMIIntegration.class)
                .map(emi -> (IngredientDragProvider) emi)
                .filter(IngredientDragProvider::isAvailable);
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
