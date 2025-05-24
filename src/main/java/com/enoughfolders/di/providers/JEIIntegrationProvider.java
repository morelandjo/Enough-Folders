package com.enoughfolders.di.providers;

import com.enoughfolders.di.IntegrationProvider;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import net.neoforged.fml.ModList;

/**
 * Provider for JEI integration.
 */
public class JEIIntegrationProvider implements IntegrationProvider<JEIIntegration> {
    
    /**
     * Creates a new JEI integration provider.
     */
    public JEIIntegrationProvider() {
        // Default constructor
    }
    
    private static final String MOD_ID = "jei";
    
    @Override
    public Class<JEIIntegration> getIntegrationType() {
        return JEIIntegration.class;
    }
    
    @Override
    public JEIIntegration create() {
        return new JEIIntegration();
    }
    
    @Override
    public boolean isAvailable() {
        // Check if JEI mod is loaded
        boolean modLoaded = ModList.get().isLoaded(MOD_ID);
        
        // Check if JEI classes are available
        if (modLoaded) {
            try {
                Class.forName("mezz.jei.api.runtime.IJeiRuntime");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        
        return false;
    }
}
