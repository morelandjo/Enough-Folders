package com.enoughfolders.integrations.jei.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.di.DependencyProvider;
import com.enoughfolders.integrations.jei.gui.handlers.FolderScreenHandler;
import com.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler;
import com.enoughfolders.integrations.jei.gui.handlers.RecipeScreenHandler;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * JEI Plugin
 */
@JeiPlugin
public class JEIPlugin implements IModPlugin {
    /**
     * The unique identifier for the JEI plugin
     */
    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(EnoughFolders.MOD_ID, "jei_plugin");
    
    /**
     * Creates a new JEI plugin instance.
     */
    public JEIPlugin() {
        EnoughFolders.LOGGER.info("DIAGNOSIS: JEIPlugin constructor called");
    }
    
    /**
     * Gets the unique identifier for this plugin.
     *
     * @return The plugin's unique ResourceLocation
     */
    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }
    
    /**
     * Called by JEI when the runtime becomes available.
     *
     * @param jeiRuntime The JEI runtime instance
     */
    @Override
    public void onRuntimeAvailable(@Nonnull IJeiRuntime jeiRuntime) {
        EnoughFolders.LOGGER.info("DIAGNOSIS: JEI Runtime becoming available - registering with integration");
        // Get JEI integration and provide it with the runtime
        DependencyProvider.get(JEIIntegration.class)
                .ifPresent(integration -> integration.setJeiRuntime(jeiRuntime));
        
        EnoughFolders.LOGGER.info("JEI Runtime available, plugin integration active");
    }
    
    /**
     * Registers GUI handlers with JEI.
     *
     * @param registration The JEI registration object for GUI handlers
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void registerGuiHandlers(@Nonnull IGuiHandlerRegistration registration) {
        EnoughFolders.LOGGER.info("DIAGNOSIS: JEIPlugin.registerGuiHandlers called - registering factory-created handlers");
        
        try {
            // Use factory to create folder screen handler
            try {
                mezz.jei.api.gui.handlers.IGuiContainerHandler folderHandler = 
                    com.enoughfolders.integrations.factory.HandlerFactory.createHandler(
                        com.enoughfolders.integrations.factory.IntegrationFactory.IntegrationType.JEI,
                        com.enoughfolders.integrations.factory.HandlerFactory.HandlerType.FOLDER_SCREEN,
                        mezz.jei.api.gui.handlers.IGuiContainerHandler.class
                    );
                registration.addGuiContainerHandler((Class) AbstractContainerScreen.class, folderHandler);
                EnoughFolders.LOGGER.info("Registered factory-created FolderScreenHandler for AbstractContainerScreen");
            } catch (Exception e) {
                EnoughFolders.LOGGER.warn("Failed to create factory-based folder screen handler, falling back to direct instantiation: " + e.getMessage());
                registration.addGuiContainerHandler((Class) AbstractContainerScreen.class, new FolderScreenHandler());
            }
            
            // Use factory to create recipe GUI handler
            try {
                mezz.jei.api.gui.handlers.IGuiContainerHandler recipeHandler = 
                    com.enoughfolders.integrations.factory.HandlerFactory.createHandler(
                        com.enoughfolders.integrations.factory.IntegrationFactory.IntegrationType.JEI,
                        com.enoughfolders.integrations.factory.HandlerFactory.HandlerType.RECIPE_GUI,
                        mezz.jei.api.gui.handlers.IGuiContainerHandler.class
                    );
                registration.addGuiContainerHandler((Class) AbstractContainerScreen.class, recipeHandler);
                EnoughFolders.LOGGER.info("Registered factory-created JEIRecipeGuiHandler for recipe GUI tracking");
            } catch (Exception e) {
                EnoughFolders.LOGGER.warn("Failed to create factory-based recipe GUI handler, falling back to direct instantiation: " + e.getMessage());
                registration.addGuiContainerHandler((Class) AbstractContainerScreen.class, new JEIRecipeGuiHandler<>());
            }
            
            // Register global handler for recipe screens and folder UI rendering
            RecipeScreenHandler globalHandler = new RecipeScreenHandler();
            registration.addGlobalGuiHandler(globalHandler);
            EnoughFolders.LOGGER.info("Registered RecipeScreenHandler as global GUI handler");
            
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error registering JEI handlers", e);
        }
    }
}
