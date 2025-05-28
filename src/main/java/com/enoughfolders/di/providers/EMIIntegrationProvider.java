/*
 * TEMPORARILY DISABLED FOR 1.21.4 - EMI hasn't updated yet
 * This file will be re-enabled once EMI supports Minecraft 1.21.4
 */
package com.enoughfolders.di.providers;

// All content commented out until EMI supports 1.21.4

/*
import com.enoughfolders.di.IntegrationProvider;
import com.enoughfolders.integrations.emi.core.EMIIntegration;
import net.neoforged.fml.ModList;

public class EMIIntegrationProvider implements IntegrationProvider<EMIIntegration> {
    
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
*/
