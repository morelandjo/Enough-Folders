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

        // Use the base handler with JEI-specific ingredient retriever
        BaseAddToFolderHandler.handleAddToFolderKeyPress("JEI", new JEIIngredientRetriever(jeiIntegration.get()));
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
            try {
                Optional<IJeiRuntime> runtimeOpt = jeiIntegration.getJeiRuntime();
                if (runtimeOpt.isEmpty()) {
                    return Optional.empty();
                }

                IJeiRuntime runtime = runtimeOpt.get();
                IIngredientListOverlay overlay = runtime.getIngredientListOverlay();

                if (!overlay.isListDisplayed()) {
                    return Optional.empty();
                }

                Optional<ITypedIngredient<?>> ingredientOpt = overlay.getIngredientUnderMouse();
                return ingredientOpt.map(ITypedIngredient::getIngredient);
            } catch (Exception e) {
                EnoughFolders.LOGGER.error("Error getting JEI ingredient under cursor", e);
                return Optional.empty();
            }
        }

        @Override
        public Optional<StoredIngredient> convertToStoredIngredient(Object ingredient) {
            return jeiIntegration.storeIngredient(ingredient);
        }

        @Override
        public boolean isOverlayVisible() {
            try {
                Optional<IJeiRuntime> runtimeOpt = jeiIntegration.getJeiRuntime();
                if (runtimeOpt.isEmpty()) {
                    return false;
                }

                IJeiRuntime runtime = runtimeOpt.get();
                return runtime.getIngredientListOverlay().isListDisplayed();
            } catch (Exception e) {
                EnoughFolders.LOGGER.error("Error checking JEI overlay visibility", e);
                return false;
            }
        }
    }
}
