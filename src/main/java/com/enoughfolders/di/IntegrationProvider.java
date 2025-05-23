package com.enoughfolders.di;

import com.enoughfolders.integrations.ModIntegration;
import java.util.function.Supplier;

/**
 * Interface for providing mod integrations.
 * <p>
 * This interface allows for delayed or conditional creation of mod integrations,
 * which enables better dependency management and testing.
 * </p>
 */
public interface IntegrationProvider<T extends ModIntegration> {
    
    /**
     * Get the class of integration this provider creates.
     * 
     * @return The class of integration
     */
    Class<T> getIntegrationType();
    
    /**
     * Create a new instance of the integration.
     * 
     * @return A new instance of the integration
     */
    T create();
    
    /**
     * Check if this integration is available in the current environment.
     * 
     * @return True if the integration can be created, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Factory method to create an IntegrationProvider from a supplier function.
     * 
     * @param <T> The type of integration
     * @param type The class of integration
     * @param factory The supplier function that creates the integration
     * @param checkAvailability Function to check if the integration is available
     * @return An IntegrationProvider for the specified integration type
     */
    static <T extends ModIntegration> IntegrationProvider<T> of(
            Class<T> type, 
            Supplier<T> factory,
            Supplier<Boolean> checkAvailability) {
        return new IntegrationProvider<T>() {
            @Override
            public Class<T> getIntegrationType() {
                return type;
            }
            
            @Override
            public T create() {
                return factory.get();
            }
            
            @Override
            public boolean isAvailable() {
                return checkAvailability.get();
            }
        };
    }
}
