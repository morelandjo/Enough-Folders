package com.enoughfolders.integrations.rei.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.util.DebugLogger;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * REI plugin for EnoughFolders mod.
 */
@OnlyIn(Dist.CLIENT)
@REIPluginClient
public class REIPlugin implements REIClientPlugin {
    
    // Flag to track if we've already logged the exclusion zones
    private static boolean exclusionZonesRegistered = false;
    
    // Initialize static field here to make sure it's loaded
    private static final String STATIC_INIT_MESSAGE = initStaticMessage();
    
    private static String initStaticMessage() {
        EnoughFolders.LOGGER.info("[EnoughFolders/REIPlugin] Static initialization method called");
        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Static initialization method called");
        DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, "Using ClassLoader: {}", REIPlugin.class.getClassLoader().getClass().getName());
        return "INITIALIZED";
    }
    
    static {
        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Static block reached");
    }
    
    /**
     * Constructor for REI plugin registration.
     */
    public REIPlugin() {
        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Constructor called");
        DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, "Static init message: {}", STATIC_INIT_MESSAGE);
    }
    
    /**
     * Called when the client is starting up and REI is being initialized.
     */
    @Override
    public void preStage(me.shedaniel.rei.api.common.plugins.PluginManager<REIClientPlugin> manager, me.shedaniel.rei.api.common.registry.ReloadStage stage) {
        DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
            "REI plugin pre-stage initialization for stage: {}", stage);
    }
    
    /**
     * Called after REI has been initialized.
     */
    @Override
    public void postStage(me.shedaniel.rei.api.common.plugins.PluginManager<REIClientPlugin> manager, me.shedaniel.rei.api.common.registry.ReloadStage stage) {
        DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
            "REI plugin post-stage initialization for stage: {}", stage);
    }
    
    /**
     * Register our screen handlers and other UI components with REI.
     * 
     * @param registry The screen registry
     */
    @Override
    public void registerScreens(me.shedaniel.rei.api.client.registry.screen.ScreenRegistry registry) {
        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
            "Registering screens for REI integration");
        
        // Create a proper exclusion zone provider
        me.shedaniel.rei.api.client.registry.screen.ExclusionZonesProvider<net.minecraft.client.gui.screens.Screen> folderExclusionZoneProvider = 
            new me.shedaniel.rei.api.client.registry.screen.ExclusionZonesProvider<net.minecraft.client.gui.screens.Screen>() {
                @Override
                public java.util.Collection<me.shedaniel.math.Rectangle> provide(net.minecraft.client.gui.screens.Screen screen) {
                    List<me.shedaniel.math.Rectangle> areas = new ArrayList<>();
                    
                    // Try to get folder screen from the current screen for container screens
                    if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) {
                        com.enoughfolders.client.event.ClientEventHandler.getFolderScreen(
                            (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) screen)
                            .ifPresent(folderScreen -> {
                                // Get the screen area and convert to REI's Rectangle format
                                net.minecraft.client.renderer.Rect2i screenArea = folderScreen.getScreenArea();
                                
                                // Add the area with a slight buffer to ensure REI doesn't overlap
                                areas.add(new me.shedaniel.math.Rectangle(
                                    screenArea.getX() - 2,
                                    screenArea.getY() - 2,
                                    screenArea.getWidth() + 4,
                                    screenArea.getHeight() + 4
                                ));
                                
                                DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION, 
                                    "Added folder UI exclusion zone for container screen: {}x{} at {},{}",
                                    screenArea.getWidth(), screenArea.getHeight(), 
                                    screenArea.getX(), screenArea.getY());
                            });
                    } else {
                        // For REI recipe screens, get the saved folder screen from the handler
                        com.enoughfolders.integrations.rei.gui.handlers.REIRecipeGuiHandler.getLastFolderScreen()
                            .ifPresent(folderScreen -> {
                                // Get the screen area and convert to REI's Rectangle format
                                net.minecraft.client.renderer.Rect2i screenArea = folderScreen.getScreenArea();
                                
                                // Add the area with a slight buffer to ensure REI doesn't overlap
                                areas.add(new me.shedaniel.math.Rectangle(
                                    screenArea.getX() - 2,
                                    screenArea.getY() - 2,
                                    screenArea.getWidth() + 4,
                                    screenArea.getHeight() + 4
                                ));
                                
                                DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION, 
                                    "Added folder UI exclusion zone for REI recipe screen: {}x{} at {},{} for screen type: {}",
                                    screenArea.getWidth(), screenArea.getHeight(), 
                                    screenArea.getX(), screenArea.getY(),
                                    screen.getClass().getSimpleName());
                            });
                    }
                    
                    return areas;
                }
            };
        
        // Register exclusion zones for container screens
        registry.exclusionZones().register(
            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class, 
            folderExclusionZoneProvider
        );
        
        // Register exclusion zones for REI recipe screens
        try {
            // DefaultDisplayViewingScreen - main REI recipe viewing screen
            Class<?> defaultDisplayViewingScreenClass = Class.forName("me.shedaniel.rei.impl.client.gui.screen.DefaultDisplayViewingScreen");
            registry.exclusionZones().register(defaultDisplayViewingScreenClass, folderExclusionZoneProvider);
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "Registered exclusion zones for DefaultDisplayViewingScreen");
        } catch (ClassNotFoundException e) {
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "DefaultDisplayViewingScreen class not found: " + e.getMessage());
        }
        
        try {
            // ViewsScreen - REI recipe search and browsing screen
            Class<?> viewsScreenClass = Class.forName("me.shedaniel.rei.impl.client.view.ViewsScreen");
            registry.exclusionZones().register(viewsScreenClass, folderExclusionZoneProvider);
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "Registered exclusion zones for ViewsScreen");
        } catch (ClassNotFoundException e) {
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "ViewsScreen class not found: " + e.getMessage());
        }
        
        try {
            // Also try to register for any general REI screen base class if it exists
            Class<?> reiScreenClass = Class.forName("me.shedaniel.rei.impl.client.gui.screen.AbstractDisplayViewingScreen");
            registry.exclusionZones().register(reiScreenClass, folderExclusionZoneProvider);
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "Registered exclusion zones for AbstractDisplayViewingScreen");
        } catch (ClassNotFoundException e) {
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "AbstractDisplayViewingScreen class not found: " + e.getMessage());
        }
        
        if (!exclusionZonesRegistered) {
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "Registered exclusion zones for folder UI on all REI screens");
            exclusionZonesRegistered = true;
        }
    }
    
    /**
     * Register transfer handlers for REI.
     * 
     * @param registry The transfer handler registry
     */
    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
            "Drag and drop functionality has been removed - not registering transfer handlers");
    }
}
