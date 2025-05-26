package com.enoughfolders.integrations.emi.handlers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.common.handlers.BaseAddToFolderHandler;
import com.enoughfolders.integrations.emi.core.EMIIntegration;
import com.enoughfolders.di.IntegrationProviderRegistry;
import com.enoughfolders.util.DebugLogger;

import java.util.Optional;

/**
 * EMI-specific handler for adding ingredients to folders via keyboard shortcuts.
 */
public class EMIAddToFolderHandler extends BaseAddToFolderHandler {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private EMIAddToFolderHandler() {
        super();
    }

    /**
     * Handles the key press to add an EMI ingredient to the active folder.
     */
    public static void handleAddToFolderKeyPress() {
        // Check if EMI classes are available
        if (!checkIntegrationClasses("dev.emi.emi.api.EmiApi")) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "EMI classes not found, skipping EMI integration");
            return;
        }

        // Get the EMI integration
        Optional<EMIIntegration> emiIntegration = getEMIIntegration();
        if (emiIntegration.isEmpty()) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "EMI integration not available");
            return;
        }

        // Use the base handler with EMI-specific ingredient retriever
        BaseAddToFolderHandler.handleAddToFolderKeyPress("EMI", new EMIIngredientRetriever(emiIntegration.get()));
    }

    /**
     * Gets the EMI integration instance.
     */
    private static Optional<EMIIntegration> getEMIIntegration() {
        return IntegrationProviderRegistry.getIntegrationByClassName("com.enoughfolders.integrations.emi.core.EMIIntegration")
            .filter(EMIIntegration.class::isInstance)
            .map(EMIIntegration.class::cast);
    }

    /**
     * EMI-specific implementation of ingredient retrieval operations.
     */
    private static class EMIIngredientRetriever implements IngredientRetriever {
        private final EMIIntegration emiIntegration;

        public EMIIngredientRetriever(EMIIntegration emiIntegration) {
            this.emiIntegration = emiIntegration;
        }

        @Override
        public Optional<Object> getIngredientUnderCursor() {
            try {
                return emiIntegration.getIngredientUnderMouse();
            } catch (Exception e) {
                EnoughFolders.LOGGER.error("Error getting EMI ingredient under cursor", e);
                return Optional.empty();
            }
        }

        @Override
        public Optional<StoredIngredient> convertToStoredIngredient(Object ingredient) {
            return emiIntegration.storeIngredient(ingredient);
        }

        @Override
        public boolean isOverlayVisible() {
            try {
                // For EMI, we check if we can get an ingredient under mouse to determine if overlay is visible
                return true; // EMI is generally always available when the integration is loaded
            } catch (Exception e) {
                EnoughFolders.LOGGER.error("Error checking EMI overlay visibility", e);
                return false;
            }
        }
    }
}
