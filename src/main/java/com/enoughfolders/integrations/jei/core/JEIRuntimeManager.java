package com.enoughfolders.integrations.jei.core;

import com.enoughfolders.integrations.base.AbstractRuntimeManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;

import java.util.Optional;

/**
 * Manages the JEI runtime instance, which is provided by JEI when it's initialized.
 */
public class JEIRuntimeManager extends AbstractRuntimeManager<IJeiRuntime> {
    
    /**
     * Creates a new JEI runtime manager.
     */
    public JEIRuntimeManager() {
        super("JEI");
    }
    
    /**
     * Sets the JEI runtime reference.
     *
     * @param jeiRuntime The JEI runtime instance
     */
    public void setJeiRuntime(IJeiRuntime jeiRuntime) {
        setRuntime(jeiRuntime);
    }
    
    /**
     * Gets the JEI runtime reference.
     *
     * @return Optional containing the JEI runtime, or empty if not available
     */
    public Optional<IJeiRuntime> getJeiRuntime() {
        return getRuntime();
    }
    /**
     * Gets the JEI recipes GUI from the runtime.
     *
     * @return Optional containing the recipes GUI, or empty if not available
     */
    public Optional<IRecipesGui> getRecipesGui() {
        return getRuntime()
            .map(IJeiRuntime::getRecipesGui);
    }
}
