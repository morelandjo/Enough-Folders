package com.enoughfolders.integrations.jei.core;

import com.enoughfolders.EnoughFolders;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;

import java.util.Optional;

/**
 * Manages the JEI runtime instance, which is provided by JEI when it's initialized.
 */
public class JEIRuntimeManager {
    
    /**
     * The JEI runtime, obtained from the JEIPlugin
     */
    private IJeiRuntime jeiRuntime;
    
    /**
     * Sets the JEI runtime reference.
     *
     * @param jeiRuntime The JEI runtime instance
     */
    public void setJeiRuntime(IJeiRuntime jeiRuntime) {
        this.jeiRuntime = jeiRuntime;
        EnoughFolders.LOGGER.info("JEI Runtime set in RuntimeManager");
    }
    
    /**
     * Gets the JEI runtime reference.
     *
     * @return Optional containing the JEI runtime, or empty if not available
     */
    public Optional<IJeiRuntime> getJeiRuntime() {
        return Optional.ofNullable(jeiRuntime);
    }
    
    /**
     * Checks if the JEI runtime is available.
     *
     * @return true if the JEI runtime is available, false otherwise
     */
    public boolean hasRuntime() {
        return jeiRuntime != null;
    }
    
    /**
     * Gets the JEI recipes GUI from the runtime.
     *
     * @return Optional containing the recipes GUI, or empty if not available
     */
    public Optional<IRecipesGui> getRecipesGui() {
        if (!hasRuntime()) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(jeiRuntime.getRecipesGui());
    }
}
