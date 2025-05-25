package com.enoughfolders.di.providers;

import com.enoughfolders.di.IntegrationProvider;
import com.enoughfolders.integrations.emi.core.EMIIntegration;
import net.neoforged.fml.ModList;

/**
 * Provider for EMI integration.
 */
public class EMIIntegrationProvider implements IntegrationProvider<EMIIntegration> {
    
    /**
     * Creates a new EMI integration provider.
     */
    public EMIIntegrationProvider() {
        // Default constructor
    }
    
    private static final String MOD_ID = "emi";
    
    @Override
    public Class<EMIIntegration> getIntegrationType() {
        return EMIIntegration.class;
    }
    
    @Override
    public EMIIntegration create() {
        return new EMIIntegration();
    }
    
    @Override
    public boolean isAvailable() {
        // Check if EMI mod is loaded
        boolean modLoaded = ModList.get().isLoaded(MOD_ID);
        
        // Check if EMI classes are available
        if (modLoaded) {
            try {
                Class.forName("dev.emi.emi.api.EmiApi");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        
        return false;
    }
}
