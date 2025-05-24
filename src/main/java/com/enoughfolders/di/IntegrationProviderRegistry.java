package com.enoughfolders.di;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.di.providers.EMIIntegrationProvider;
import com.enoughfolders.di.providers.JEIIntegrationProvider;
import com.enoughfolders.di.providers.REIIntegrationProvider;
import com.enoughfolders.integrations.ModIntegration;
import com.enoughfolders.integrations.api.IngredientDragProvider;
import com.enoughfolders.integrations.api.RecipeViewingIntegration;
import com.enoughfolders.integrations.ftb.core.FTBLibraryIntegration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Registry for managing integration providers through dependency injection.
 * <p>
 * This class serves as a bridge between the old IntegrationRegistry and the new DI system,
 * allowing for a gradual transition to dependency injection.
 * </p>
 */
public class IntegrationProviderRegistry {
    
    private static final List<IntegrationProvider<?>> providers = new ArrayList<>();
    private static final List<ModIntegration> instances = new ArrayList<>();
    private static boolean initialized = false;
    
    /**
     * Initialize the registry with standard integration providers.
     */
    public static synchronized void initialize() {
        if (initialized) {
            EnoughFolders.LOGGER.debug("IntegrationProviderRegistry already initialized, skipping");
            return;
        }
        
        // Check for system properties that affect which integrations to load
        boolean jeiOnly = Boolean.parseBoolean(System.getProperty("enoughfolders.jei_only", "false"));
        boolean reiOnly = Boolean.parseBoolean(System.getProperty("enoughfolders.rei_only", "false"));
        
        EnoughFolders.LOGGER.info("EnoughFolders: System property 'enoughfolders.jei_only' = {}", jeiOnly);
        EnoughFolders.LOGGER.info("EnoughFolders: System property 'enoughfolders.rei_only' = {}", reiOnly);
        
        // Register JEI integration provider
        if (!reiOnly) {
            registerProvider(new JEIIntegrationProvider());
        } else {
            EnoughFolders.LOGGER.info("Running in REI-only mode, JEI integration disabled");
        }
        
        // Register FTB Library integration
        registerSingleton(new FTBLibraryIntegration());
         // Register REI integration provider
        if (!jeiOnly) {
            registerProvider(new REIIntegrationProvider());
        } else {
            EnoughFolders.LOGGER.info("Running in JEI-only mode, REI integration disabled");
        }

        // Register EMI integration provider
        if (!jeiOnly && !reiOnly) {
            registerProvider(new EMIIntegrationProvider());
        } else {
            EnoughFolders.LOGGER.info("Running in exclusive mode, EMI integration disabled");
        }

        // Initialize all registered integrations
        instances.forEach(integration -> {
            integration.initialize();
            integration.registerDragAndDrop();
        });
        
        initialized = true;
        
        EnoughFolders.LOGGER.info("IntegrationProviderRegistry initialization complete, integrations count: {}", instances.size());
    }
    
    /**
     * Register an integration provider.
     * 
     * @param provider The provider to register
     */
    public static <T extends ModIntegration> void registerProvider(IntegrationProvider<T> provider) {
        if (provider.isAvailable()) {
            providers.add(provider);
            T integration = provider.create();
            instances.add(integration);
            
            // Register the integration in the DependencyProvider
            DependencyProvider.registerSingleton(provider.getIntegrationType(), integration);
            
            EnoughFolders.LOGGER.info("Registered integration: " + integration.getClass().getSimpleName());
        } else {
            EnoughFolders.LOGGER.info("Integration not available: " + provider.getIntegrationType().getSimpleName());
        }
    }
    
    /**
     * Register a singleton integration directly.
     * 
     * @param integration The singleton integration instance
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerSingleton(ModIntegration integration) {
        if (integration.isAvailable()) {
            instances.add(integration);
            // Need to use this approach to handle the generic type safely
            Class clazz = integration.getClass();
            DependencyProvider.registerSingleton(clazz, integration);
            EnoughFolders.LOGGER.info("Registered singleton integration: " + integration.getClass().getSimpleName());
        }
    }
    
    /**
     * Get an integration by its class.
     * 
     * @param <T> The integration type
     * @param integrationClass The class of the integration
     * @return Optional containing the integration if found, empty otherwise
     */
    public static <T extends ModIntegration> Optional<T> getIntegration(Class<T> integrationClass) {
        return DependencyProvider.get(integrationClass);
    }
    
    /**
     * Find integrations that match the given predicate.
     * 
     * @param predicate The predicate to match integrations against
     * @return List of matching integrations
     */
    public static List<ModIntegration> findIntegrations(Predicate<ModIntegration> predicate) {
        return instances.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    /**
     * Find integrations of a specific type.
     * 
     * @param <T> The integration type
     * @param integrationClass The class of integrations to find
     * @return List of matching integrations
     */
    @SuppressWarnings("unchecked")
    public static <T extends ModIntegration> List<T> findIntegrationsByType(Class<T> integrationClass) {
        return instances.stream()
                .filter(integration -> integrationClass.isAssignableFrom(integration.getClass()))
                .map(integration -> (T) integration)
                .collect(Collectors.toList());
    }
    
    /**
     * Find recipe viewing integrations.
     * 
     * @return List of recipe viewing integrations
     */
    public static List<RecipeViewingIntegration> findRecipeViewingIntegrations() {
        return instances.stream()
                .filter(integration -> integration instanceof RecipeViewingIntegration)
                .map(integration -> (RecipeViewingIntegration) integration)
                .collect(Collectors.toList());
    }
    
    /**
     * Find ingredient drag providers.
     * 
     * @return List of ingredient drag providers
     */
    public static List<IngredientDragProvider> findIngredientDragProviders() {
        return instances.stream()
                .filter(integration -> integration instanceof IngredientDragProvider)
                .map(integration -> (IngredientDragProvider) integration)
                .collect(Collectors.toList());
    }
    
    /**
     * Check if an integration with the given class name exists.
     * 
     * @param className The fully qualified class name
     * @return true if an integration with the given class name exists, false otherwise
     */
    public static boolean hasIntegrationWithClassName(String className) {
        return instances.stream()
                .anyMatch(integration -> integration.getClass().getName().equals(className));
    }
    
    /**
     * Get an integration by its class name.
     * 
     * @param className The fully qualified class name
     * @return Optional containing the integration if found, empty otherwise
     */
    public static Optional<ModIntegration> getIntegrationByClassName(String className) {
        return instances.stream()
                .filter(integration -> integration.getClass().getName().equals(className))
                .findFirst();
    }
    
    /**
     * Get all registered integration instances.
     * 
     * @return List of all integration instances
     */
    public static List<ModIntegration> getAllIntegrations() {
        return new ArrayList<>(instances);
    }
}
