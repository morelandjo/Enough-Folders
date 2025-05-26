package com.enoughfolders.integrations.factory;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.integrations.ModIntegration;
import com.enoughfolders.integrations.base.AbstractIntegration;
import com.enoughfolders.integrations.emi.core.EMIIntegration;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import com.enoughfolders.integrations.rei.core.REIIntegration;
import com.enoughfolders.integrations.util.PluginUtils;
import com.enoughfolders.integrations.util.RuntimeUtils;
import com.enoughfolders.util.DebugLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Factory class for creating integration instances with centralized initialization logic.
 * Provides common initialization patterns, dependency injection support, and error handling.
 */
public final class IntegrationFactory {

    /**
     * Registry of integration suppliers by integration type.
     */
    private static final Map<IntegrationType, Supplier<ModIntegration>> INTEGRATION_SUPPLIERS = new HashMap<>();
    
    /**
     * Cache for created integrations to ensure singleton behavior.
     */
    private static final Map<IntegrationType, ModIntegration> INTEGRATION_CACHE = new HashMap<>();
    
    /**
     * Flag to track factory initialization.
     */
    private static boolean initialized = false;

    /**
     * Enumeration of supported integration types.
     */
    public enum IntegrationType {
        /** Just Enough Items integration */
        JEI("jei", "JEI"),
        /** Enough Mods Interface integration */
        EMI("emi", "EMI"), 
        /** Roughly Enough Items integration */
        REI("rei", "REI");

        private final String id;
        private final String displayName;

        /**
         * Constructor for IntegrationType.
         *
         * @param id The string identifier for this integration type
         * @param displayName The human-readable display name
         */
        IntegrationType(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        /**
         * Gets the string identifier for this integration type.
         *
         * @return The string identifier
         */
        public String getId() {
            return id;
        }

        /**
         * Gets the human-readable display name for this integration type.
         *
         * @return The display name
         */
        public String getDisplayName() {
            return displayName;
        }
    }

    private IntegrationFactory() {
        // Utility class should not be instantiated
    }

    /**
     * Initialize the integration factory with default suppliers.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Initializing IntegrationFactory", "");

            // Register integration suppliers
            registerIntegrationSupplier(IntegrationType.JEI, JEIIntegration::new);
            registerIntegrationSupplier(IntegrationType.EMI, EMIIntegration::new);
            registerIntegrationSupplier(IntegrationType.REI, REIIntegration::new);

            initialized = true;

            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "IntegrationFactory initialized with {} integration types", 
                INTEGRATION_SUPPLIERS.size());

        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to initialize IntegrationFactory", e);
        }
    }

    /**
     * Register a custom integration supplier.
     * 
     * @param type The integration type
     * @param supplier The supplier function that creates the integration instance
     */
    public static void registerIntegrationSupplier(IntegrationType type, Supplier<ModIntegration> supplier) {
        INTEGRATION_SUPPLIERS.put(type, supplier);
        DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
            "Registered integration supplier for type: {}", type.getDisplayName());
    }

    /**
     * Create or retrieve a cached integration instance.
     * 
     * @param type The type of integration to create
     * @return Optional containing the integration instance, or empty if creation failed
     */
    public static Optional<ModIntegration> createIntegration(IntegrationType type) {
        if (!initialized) {
            initialize();
        }

        // Return cached instance if available
        if (INTEGRATION_CACHE.containsKey(type)) {
            ModIntegration cached = INTEGRATION_CACHE.get(type);
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Returning cached {} integration instance", type.getDisplayName());
            return Optional.of(cached);
        }

        // Create new instance
        Supplier<ModIntegration> supplier = INTEGRATION_SUPPLIERS.get(type);
        if (supplier == null) {
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "No supplier registered for integration type: {}", type.getDisplayName());
            return Optional.empty();
        }

        try {
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Creating new {} integration instance", type.getDisplayName());

            // Create the integration instance
            ModIntegration integration = supplier.get();
            
            // Perform common initialization
            initializeIntegration(integration, type);
            
            // Cache the instance
            INTEGRATION_CACHE.put(type, integration);
            
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Successfully created {} integration instance", type.getDisplayName());
            
            return Optional.of(integration);

        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to create {} integration", type.getDisplayName(), e);
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Integration creation failed for {}: {}", type.getDisplayName(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Create an integration by its string identifier.
     * 
     * @param integrationId The string identifier for the integration
     * @return Optional containing the integration instance, or empty if not found or creation failed
     */
    public static Optional<ModIntegration> createIntegration(String integrationId) {
        if (integrationId == null || integrationId.trim().isEmpty()) {
            return Optional.empty();
        }

        // Find matching integration type
        for (IntegrationType type : IntegrationType.values()) {
            if (type.getId().equalsIgnoreCase(integrationId.trim())) {
                return createIntegration(type);
            }
        }

        DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
            "Unknown integration ID: {}", integrationId);
        return Optional.empty();
    }

    /**
     * Perform common initialization logic for an integration.
     * 
     * @param integration The integration instance to initialize
     * @param type The integration type
     */
    private static void initializeIntegration(ModIntegration integration, IntegrationType type) {
        try {
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Performing common initialization for {} integration", type.getDisplayName());

            // Check availability first
            if (!integration.isAvailable()) {
                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                    "{} integration reports as not available", type.getDisplayName());
                return;
            }

            // Perform runtime checks if this is an AbstractIntegration
            if (integration instanceof AbstractIntegration) {
                AbstractIntegration abstractIntegration = (AbstractIntegration) integration;
                
                // Validate runtime environment
                if (!RuntimeUtils.validateRuntimeEnvironment()) {
                    DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                        "Runtime environment validation failed for {}", type.getDisplayName());
                    return;
                }
                
                // Set up error handling
                abstractIntegration.setErrorHandler(PluginUtils.createErrorHandler(type.getDisplayName()));
            }

            // Initialize the integration
            integration.initialize();

            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Common initialization completed for {} integration", type.getDisplayName());

        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to initialize {} integration", type.getDisplayName(), e);
            DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                "Initialization failed for {}: {}", type.getDisplayName(), e.getMessage());
        }
    }

    /**
     * Get all available integration types.
     * 
     * @return Array of all integration types
     */
    public static IntegrationType[] getAvailableTypes() {
        return IntegrationType.values();
    }

    /**
     * Check if an integration type is supported.
     * 
     * @param type The integration type to check
     * @return true if the type is supported, false otherwise
     */
    public static boolean isTypeSupported(IntegrationType type) {
        return INTEGRATION_SUPPLIERS.containsKey(type);
    }

    /**
     * Check if an integration ID is supported.
     * 
     * @param integrationId The integration ID to check
     * @return true if the ID is supported, false otherwise
     */
    public static boolean isIdSupported(String integrationId) {
        if (integrationId == null || integrationId.trim().isEmpty()) {
            return false;
        }
        
        for (IntegrationType type : IntegrationType.values()) {
            if (type.getId().equalsIgnoreCase(integrationId.trim())) {
                return isTypeSupported(type);
            }
        }
        return false;
    }

    /**
     * Clear all cached integration instances.
     */
    public static void clearCache() {
        DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
            "Clearing integration cache with {} instances", INTEGRATION_CACHE.size());
        INTEGRATION_CACHE.clear();
    }

    /**
     * Get statistics about the factory state.
     * 
     * @return Map containing factory statistics
     */
    public static Map<String, Object> getFactoryStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("initialized", initialized);
        stats.put("supportedTypes", INTEGRATION_SUPPLIERS.size());
        stats.put("cachedInstances", INTEGRATION_CACHE.size());
        stats.put("availableTypes", IntegrationType.values().length);
        return stats;
    }

    /**
     * Create all available integrations at once.
     * 
     * @return Map of successfully created integrations by type
     */
    public static Map<IntegrationType, ModIntegration> createAllIntegrations() {
        Map<IntegrationType, ModIntegration> created = new HashMap<>();
        
        for (IntegrationType type : IntegrationType.values()) {
            createIntegration(type).ifPresent(integration -> {
                created.put(type, integration);
                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
                    "Successfully created {} integration in batch mode", type.getDisplayName());
            });
        }
        
        DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
            "Batch creation completed: {}/{} integrations created", 
            created.size(), IntegrationType.values().length);
        
        return created;
    }
}
