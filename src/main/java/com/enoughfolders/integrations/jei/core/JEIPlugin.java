package com.enoughfolders.integrations.jei.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.jei.gui.handlers.DragDropHandler;
import com.enoughfolders.integrations.jei.gui.handlers.FolderScreenHandler;
import com.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler;
import com.enoughfolders.integrations.jei.gui.handlers.RecipeScreenHandler;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * JEI Plugin for Enough Folders.
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
        IntegrationRegistry.getIntegration(JEIIntegrationCore.class)
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
        EnoughFolders.LOGGER.info("DIAGNOSIS: JEIPlugin.registerGuiHandlers called - registering handlers");
        
        try {
            // Register container screen handler to provide exclusion zones for folders
            FolderScreenHandler containerHandler = new FolderScreenHandler();
            registration.addGuiContainerHandler((Class) AbstractContainerScreen.class, containerHandler);
            
            // Register ghost ingredient handler for regular container screens
            registration.addGhostIngredientHandler((Class) AbstractContainerScreen.class, containerHandler);
            EnoughFolders.LOGGER.info("Registered FolderScreenHandler for AbstractContainerScreen");
            
            // Register JEI recipe GUI handler for tracking folder screens
            JEIRecipeGuiHandler<?> recipeGuiHandler = new JEIRecipeGuiHandler<>();
            registration.addGuiContainerHandler((Class) AbstractContainerScreen.class, recipeGuiHandler);
            EnoughFolders.LOGGER.info("Registered JEIRecipeGuiHandler for recipe GUI tracking");
            
            // Register global handler for recipe screens and folder UI rendering
            RecipeScreenHandler globalHandler = new RecipeScreenHandler();
            registration.addGlobalGuiHandler(globalHandler);
            EnoughFolders.LOGGER.info("Registered RecipeScreenHandler as global GUI handler");
            
            // Create drag-drop handler for folder targets
            DragDropHandler dragDropHandler = new DragDropHandler();
            
            // Register for RecipesGui specifically
            try {
                // Try to get JEI's RecipesGui class
                Class<?> recipesGuiClass = Class.forName("mezz.jei.gui.recipes.RecipesGui");
                if (Screen.class.isAssignableFrom(recipesGuiClass)) {
                    // Register for the RecipesGui class
                    EnoughFolders.LOGGER.info("Registering DragDropHandler for JEI RecipesGui");
                    registration.addGhostIngredientHandler((Class) recipesGuiClass, dragDropHandler);
                }
            } catch (ClassNotFoundException e) {
                EnoughFolders.LOGGER.warn("Could not find JEI's RecipesGui class", e);
            }
            
            // Register the handler for any ScreenWithFolderUI subclasses if present
            try {
                // Get any screens that implement FolderGhostIngredientTarget interface
                Class<?>[] screenClasses = {
                    Class.forName("com.enoughfolders.client.gui.screens.FolderConfigScreen"),
                    Class.forName("mezz.jei.gui.recipes.RecipesGui")
                };
                
                for (Class<?> screenClass : screenClasses) {
                    if (Screen.class.isAssignableFrom(screenClass)) {
                        EnoughFolders.LOGGER.info("Registering DragDropHandler for screen: {}", screenClass.getSimpleName());
                        registration.addGhostIngredientHandler((Class) screenClass, dragDropHandler);
                    }
                }
            } catch (ClassNotFoundException e) {
                EnoughFolders.LOGGER.debug("Some folder UI screen classes not found", e);
            }
            
            EnoughFolders.LOGGER.info("Registered DragDropHandler for folder targets");
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error registering JEI handlers", e);
        }
    }
}
