package com.enoughfolders.di;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.ModIntegration;

import java.util.Optional;

/**
 * Provides compatibility between the old IntegrationRegistry and the new dependency injection system.
 * <p>
 * This class ensures that code written against the old API continues to work while
 * migrating to the new dependency injection system.
 * </p>
 */
public class IntegrationRegistryBridge {
    
    private static boolean bridgeInitialized = false;
    
    /**
     * Initialize the compatibility bridge.
     * <p>
     * This method ensures that all integrations registered through the new IntegrationProviderRegistry
     * are also accessible through the old IntegrationRegistry.
     * </p>
     */
    public static synchronized void initializeBridge() {
        if (bridgeInitialized) {
            return;
        }
        
        try {
            // For each integration in the new registry, register it with the old registry
            for (ModIntegration integration : IntegrationProviderRegistry.getAllIntegrations()) {
                registerWithOldRegistry(integration);
            }
            
            bridgeInitialized = true;
            EnoughFolders.LOGGER.info("Integration registry bridge initialized");
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to initialize integration registry bridge", e);
        }
    }
    
    /**
     * Register an integration with the old IntegrationRegistry.
     * <p>
     * This uses reflection to access the private register method of IntegrationRegistry.
     * </p>
     * 
     * @param integration The integration to register
     */
    private static void registerWithOldRegistry(ModIntegration integration) {
        // Use the public register method if available
        IntegrationRegistry.register(integration);
        
        EnoughFolders.LOGGER.debug("Registered {} with old registry", integration.getClass().getSimpleName());
    }
    
    /**
     * Get an integration by class name from the new DI system.
     * <p>
     * This provides compatibility for code that uses IntegrationRegistry.getIntegrationByClassName().
     * </p>
     * 
     * @param className The fully qualified class name
     * @return Optional containing the integration if found, empty otherwise
     */
    public static Optional<ModIntegration> getIntegrationByClassName(String className) {
        return IntegrationProviderRegistry.getIntegrationByClassName(className);
    }
    
    /**
     * Check if an integration is available through the new DI system.
     * <p>
     * This provides compatibility for code that uses IntegrationRegistry.isIntegrationAvailable().
     * </p>
     * 
     * @param integrationId The ID of the integration (e.g., "jei", "rei")
     * @return true if an integration with the given ID is available, false otherwise
     */
    public static boolean isIntegrationAvailable(String integrationId) {
        if ("jei".equals(integrationId)) {
            return DependencyProvider.has(com.enoughfolders.integrations.jei.core.JEIIntegration.class);
        } else if ("rei".equals(integrationId)) {
            return DependencyProvider.has(com.enoughfolders.integrations.rei.core.REIIntegration.class);
        } else if ("emi".equals(integrationId)) {
            return DependencyProvider.has(com.enoughfolders.integrations.emi.core.EMIIntegration.class);
        }
        
        return false;
    }
}
