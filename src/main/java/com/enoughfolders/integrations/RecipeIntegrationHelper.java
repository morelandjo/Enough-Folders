package com.enoughfolders.integrations;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import com.enoughfolders.integrations.rei.core.REIIntegration;
import com.enoughfolders.integrations.emi.core.EMIIntegration;
import com.enoughfolders.util.DebugLogger;
import com.enoughfolders.di.IntegrationProviderRegistry;
import com.enoughfolders.di.DependencyProvider;

import java.util.Optional;

/**
 * Utility class for handling integration priorities and operations
 */
/**
 * Helper utility for common recipe integration operations across different recipe viewer mods.
 */
public class RecipeIntegrationHelper {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private RecipeIntegrationHelper() {
        // Utility class should not be instantiated
    }
    /**
     * Integration priority order, highest priority first.
     */
    private static final String[] PRIORITY_ORDER = {
        "jei",   
        "rei",
        "emi"
    };
    
    /**
     * Check if any recipe viewing mod is available.
     * 
     * @return true if at least one recipe viewing mod is available
     */
    public static boolean isRecipeViewingAvailable() {
        return IntegrationProviderRegistry.hasIntegrationWithShortId("rei") ||
               IntegrationProviderRegistry.hasIntegrationWithShortId("jei") ||
               IntegrationProviderRegistry.hasIntegrationWithShortId("emi");
    }
    
    /**
     * Show recipes for an ingredient using the highest priority available mod.
     * 
     * @param ingredient The ingredient to show recipes for
     * @return true if recipes were shown, false otherwise
     */
    public static boolean showRecipes(StoredIngredient ingredient) {
        // Try integrations in priority order
        for (String integrationId : PRIORITY_ORDER) {
            if (IntegrationProviderRegistry.hasIntegrationWithShortId(integrationId)) {
                if (tryShowRecipes(integrationId, ingredient)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Show uses for an ingredient using the highest priority available mod.
     * 
     * @param ingredient The ingredient to show uses for
     * @return true if uses were shown, false otherwise
     */
    public static boolean showUses(StoredIngredient ingredient) {
        // Try integrations in priority order
        for (String integrationId : PRIORITY_ORDER) {
            if (IntegrationProviderRegistry.hasIntegrationWithShortId(integrationId)) {
                if (tryShowUses(integrationId, ingredient)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Try to show recipes using a specific integration.
     * 
     * @param integrationId The ID of the integration to use
     * @param ingredient The ingredient to show recipes for
     * @return true if recipes were shown, false otherwise
     */
    private static boolean tryShowRecipes(String integrationId, StoredIngredient ingredient) {
        try {
            if ("rei".equals(integrationId)) {
                return DependencyProvider.get(REIIntegration.class)
                    .map(rei -> {
                        Optional<?> reiIngredient = rei.getIngredientFromStored(ingredient);
                        if (reiIngredient.isPresent()) {
                            rei.showRecipes(reiIngredient.get());
                            return true;
                        }
                        return false;
                    }).orElse(false);
            } else if ("jei".equals(integrationId)) {
                return DependencyProvider.get(JEIIntegration.class)
                    .map(jei -> {
                        Optional<?> jeiIngredient = jei.getIngredientFromStored(ingredient);
                        if (jeiIngredient.isPresent()) {
                            jei.showRecipes(jeiIngredient.get());
                            return true;
                        }
                        return false;
                    }).orElse(false);
            } else if ("emi".equals(integrationId)) {
                return DependencyProvider.get(EMIIntegration.class)
                    .map(emi -> {
                        Optional<?> emiIngredient = emi.getIngredientFromStored(ingredient);
                        if (emiIngredient.isPresent()) {
                            emi.showRecipes(emiIngredient.get());
                            return true;
                        }
                        return false;
                    }).orElse(false);
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error showing recipes with {}: {}", 
                integrationId, e.getMessage());
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Exception details: {}", e);
        }
        return false;
    }
    
    /**
     * Try to show uses using a specific integration.
     * 
     * @param integrationId The ID of the integration to use
     * @param ingredient The ingredient to show uses for
     * @return true if uses were shown, false otherwise
     */
    private static boolean tryShowUses(String integrationId, StoredIngredient ingredient) {
        try {
            if ("rei".equals(integrationId)) {
                return DependencyProvider.get(REIIntegration.class)
                    .map(rei -> {
                        Optional<?> reiIngredient = rei.getIngredientFromStored(ingredient);
                        if (reiIngredient.isPresent()) {
                            rei.showUses(reiIngredient.get());
                            return true;
                        }
                        return false;
                    }).orElse(false);
            } else if ("jei".equals(integrationId)) {
                return DependencyProvider.get(JEIIntegration.class)
                    .map(jei -> {
                        Optional<?> jeiIngredient = jei.getIngredientFromStored(ingredient);
                        if (jeiIngredient.isPresent()) {
                            jei.showUses(jeiIngredient.get());
                            return true;
                        }
                        return false;
                    }).orElse(false);
            } else if ("emi".equals(integrationId)) {
                return DependencyProvider.get(EMIIntegration.class)
                    .map(emi -> {
                        Optional<?> emiIngredient = emi.getIngredientFromStored(ingredient);
                        if (emiIngredient.isPresent()) {
                            emi.showUses(emiIngredient.get());
                            return true;
                        }
                        return false;
                    }).orElse(false);
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error showing uses with {}: {}", 
                integrationId, e.getMessage());
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Exception details: {}", e);
        }
        return false;
    }
}
