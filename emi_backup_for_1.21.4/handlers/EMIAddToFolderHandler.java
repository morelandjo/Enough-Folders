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
                Optional<Object> ingredient = emiIntegration.getIngredientUnderMouse();
                
                if (ingredient.isEmpty()) {
                    DebugLogger.debug(DebugLogger.Category.INTEGRATION, "No EMI ingredient found under cursor");
                } else {
                    DebugLogger.debug(DebugLogger.Category.INTEGRATION, "Found EMI ingredient under cursor: " + ingredient.get().getClass().getName());
                }
                
                return ingredient;
            } catch (Exception e) {
                EnoughFolders.LOGGER.error("Error getting EMI ingredient under cursor", e);
                DebugLogger.debug(DebugLogger.Category.INTEGRATION, "Exception getting EMI ingredient: " + e.getMessage());
                return Optional.empty();
            }
        }

        @Override
        public Optional<StoredIngredient> convertToStoredIngredient(Object ingredient) {
            try {
                Optional<StoredIngredient> result = emiIntegration.storeIngredient(ingredient);
                if (result.isEmpty()) {
                    DebugLogger.debug(DebugLogger.Category.INTEGRATION, "Failed to convert EMI ingredient to StoredIngredient");
                } else {
                    StoredIngredient stored = result.get();
                    DebugLogger.debug(DebugLogger.Category.INTEGRATION, "Converted EMI ingredient to StoredIngredient: type=" + 
                        stored.getType() + ", value=" + stored.getValue());
                }
                return result;
            } catch (Exception e) {
                EnoughFolders.LOGGER.error("Error converting EMI ingredient", e);
                DebugLogger.debug(DebugLogger.Category.INTEGRATION, "Exception converting EMI ingredient: " + e.getMessage());
                return Optional.empty();
            }
        }

        @Override
        public boolean isOverlayVisible() {
            try {
                // Check if we can get an ingredient under mouse to determine if overlay is visible
                Class<?> emiApiClass = Class.forName("dev.emi.emi.api.EmiApi");
                
                // Try to check if search is focused - this is a reliable way to know EMI is active
                try {
                    java.lang.reflect.Method isSearchFocusedMethod = emiApiClass.getMethod("isSearchFocused");
                    Boolean isSearchFocused = (Boolean) isSearchFocusedMethod.invoke(null);
                    if (isSearchFocused) {
                        return true;
                    }
                } catch (NoSuchMethodException e) {
                    // Method doesn't exist, continue with other checks
                }
                
                // Try to get a hovered stack as another way to check if EMI is active
                java.lang.reflect.Method getHoveredStackMethod = emiApiClass.getMethod("getHoveredStack", boolean.class);
                Object emiStackInteraction = getHoveredStackMethod.invoke(null, true);
                return emiStackInteraction != null;
            } catch (Exception e) {
                EnoughFolders.LOGGER.error("Error checking EMI overlay visibility", e);
                return false;
            }
        }
    }
}
