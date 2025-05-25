package com.enoughfolders;

import com.enoughfolders.client.event.ClientEventHandler;
import com.enoughfolders.client.input.KeyBindings;
import com.enoughfolders.client.input.KeyHandler;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.di.DependencyProvider;
import com.enoughfolders.di.IntegrationProviderRegistry;
import com.enoughfolders.util.DebugConfig;
import com.enoughfolders.util.DebugLogger;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main mod class for Enough Folders.
 */
@Mod(EnoughFolders.MOD_ID)
public class EnoughFolders {
    /** 
     * mod ID
     */
    public static final String MOD_ID = "enoughfolders";
    
    /**
     * Logger instance
     */
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    
    /**
     * Singleton instance
     */
    private static EnoughFolders instance;
    
    /**
     * Manager for all folders data and operations
     */
    private FolderManager folderManager;

    /**
     * Constructs the main mod instance.
     *
     * @param modEventBus The mod-specific event bus for registration events
     */
    public EnoughFolders(IEventBus modEventBus) {
        instance = this;
        
        // Initialize debugging system
        DebugConfig.load();
        DebugLogger.debug(DebugLogger.Category.INITIALIZATION, "Debug configuration loaded");

        if (FMLEnvironment.dist == Dist.CLIENT) {
            // Initialize folder manager
            this.folderManager = new FolderManager();
            DebugLogger.debug(DebugLogger.Category.INITIALIZATION, "Folder manager created");
            
            // Register folder manager as a singleton
            DependencyProvider.registerSingleton(FolderManager.class, this.folderManager);
            
            // Initialize client-side event handlers and setup
            initializeClientComponents();
            
            // Initialize mod integrations
            IntegrationProviderRegistry.initialize();
        }
        
        // Register common event handlers
        NeoForge.EVENT_BUS.register(this);
        
        LOGGER.info("EnoughFolders initialized");
        DebugLogger.debug(DebugLogger.Category.INITIALIZATION, "Mod initialization complete");
    }
    
    /**
     * Event handler for registering commands.
     * 
     * @param event The command registration event
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // Command registration code (if any) goes here
    }

    /**
     * Get the mod instance.
     *
     * @return The mod instance
     */
    public static EnoughFolders getInstance() {
        return instance;
    }

    /**
     * Get the folder manager for accessing folder data and operations.
     *
     * @return The folder manager
     */
    public FolderManager getFolderManager() {
        return folderManager;
    }

    /**
     * Initialize client-side components.
     */
    private void initializeClientComponents() {
        // Initialize key bindings
        KeyBindings.init();
        DebugLogger.debug(DebugLogger.Category.INITIALIZATION, "Key bindings initialized");
        
        // Initialize client event handlers
        ClientEventHandler.initialize();
        DebugLogger.debug(DebugLogger.Category.INITIALIZATION, "Client event handler initialized");
        
        // Initialize key handler for custom key bindings
        KeyHandler.init();
        DebugLogger.debug(DebugLogger.Category.INITIALIZATION, "Key handler initialized");
        
        LOGGER.info("EnoughFolders client components initialized");
    }
}
