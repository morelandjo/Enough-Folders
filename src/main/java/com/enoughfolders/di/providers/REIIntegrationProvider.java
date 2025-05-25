package com.enoughfolders.di.providers;

import com.enoughfolders.di.IntegrationProvider;
import com.enoughfolders.integrations.rei.core.REIIntegration;
import net.neoforged.fml.ModList;

/**
 * Provider for REI integration.
 */
public class REIIntegrationProvider implements IntegrationProvider<REIIntegration> {
    
    /**
     * Creates a new REI integration provider.
     */
    public REIIntegrationProvider() {
        // Default constructor
    }
    
    private static final String MOD_ID = "roughlyenoughitems";
    
    @Override
    public Class<REIIntegration> getIntegrationType() {
        return REIIntegration.class;
    }
    
    @Override
    public REIIntegration create() {
        return new REIIntegration();
    }
    
    @Override
    public boolean isAvailable() {
        // Check if REI mod is loaded
        boolean modLoaded = ModList.get().isLoaded(MOD_ID);
        
        // Check if REI classes are available
        if (modLoaded) {
            try {
                Class.forName("me.shedaniel.rei.api.client.REIRuntime");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        
        return false;
    }
}
